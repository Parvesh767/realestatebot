package com.risingbee.realestate.automation.rent.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.automation.exception.ResourceNotFoundException;
import com.risingbee.realestate.automation.rent.domain.RentPaymentStatus;
import com.risingbee.realestate.automation.rent.domain.RentSchedule;
import com.risingbee.realestate.automation.rent.repo.RentScheduleRepository;
import com.risingbee.realestate.automation.repo.BrokerUpiHandleRepository;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RentTrackingService {

    private final RentScheduleRepository rentRepository;
    private final TenancyContractRepository contractRepository;
    private final WhatsAppSender whatsAppSender;
    private final BrokerUpiHandleRepository upiHandleRepository;

    /**
     * Generates or updates the monthly rent ledger entry
     */
    public RentSchedule generateMonthlyRentDue(
            Long contractId,
            String billingCycleMonth,
            LocalDate dueDate,
            Long electricityInr,
            Long maintenanceInr,
            Long waterInr
    ) {
        String normalizedCycle = normalizeCycle(billingCycleMonth);

        TenancyContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        long baseRent = contract.getMonthlyRent() != null ? contract.getMonthlyRent() : 0L;
        long eb = electricityInr != null ? electricityInr : 0L;
        long maint = maintenanceInr != null ? maintenanceInr : 0L;
        long water = waterInr != null ? waterInr : 0L;
        long total = baseRent + eb + maint + water;

        // Lookup using the normalized cycle string
        RentSchedule schedule = rentRepository.findByContractIdAndBillingCycleMonth(contractId, normalizedCycle)
                .orElseGet(RentSchedule::new);

        schedule.setContract(contract);
        schedule.setBillingCycleMonth(normalizedCycle);
        schedule.setDueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(5));
        schedule.setBaseRentInr(baseRent);
        schedule.setElectricityInr(eb);
        schedule.setMaintenanceInr(maint);
        schedule.setWaterInr(water);
        schedule.setTotalDueInr(total);
        if (schedule.getPaidAmountInr() == null) {
            schedule.setPaidAmountInr(0L);
        }
        if (schedule.getStatus() == null) {
            schedule.setStatus(RentPaymentStatus.PENDING);
        }

        rentRepository.save(schedule);

        // Send instant WhatsApp Invoice alert to Tenant
        sendRentDueWhatsAppNotice(schedule, contract);

        return schedule;
    }

    /**
     * Mark a rent invoice as PAID (called by Owner, Broker, or auto-reconciliation webhook)
     */
    public RentSchedule recordRentPayment(Long scheduleId, Long amountPaid, String paymentMode, String txRef) {
        RentSchedule schedule = rentRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rent schedule not found: " + scheduleId));

        long currentPaid = amountPaid != null ? amountPaid : 0L;
        schedule.setPaidAmountInr(currentPaid);
        schedule.setPaidAt(Instant.now());
        schedule.setPaymentMode(paymentMode);
        schedule.setTransactionReference(txRef);

        long totalDue = schedule.getTotalDueInr() != null ? schedule.getTotalDueInr() : 0L;
        if (currentPaid >= totalDue) {
            schedule.setStatus(RentPaymentStatus.PAID);
        } else {
            schedule.setStatus(RentPaymentStatus.PARTIALLY_PAID);
        }

        rentRepository.save(schedule);

        // Send unified WhatsApp payment notices
        TenancyContract contract = schedule.getContract();
        if (contract != null) {
            sendPaymentReceiptNotices(contract, schedule, currentPaid, paymentMode, txRef);
        }

        return schedule;
    }

    /**
     * Daily Cron Job at 09:00 AM to scan for overdue rents & send reminders
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void executeDailyRentReminders() {
        LocalDate today = LocalDate.now();
        List<RentSchedule> pending = rentRepository.findPendingRentsForReminder(today.plusDays(2));

        for (RentSchedule rent : pending) {
            TenancyContract contract = rent.getContract();
            if (contract == null) continue;

            // Mark OVERDUE if past due date
            if (rent.getDueDate() != null && rent.getDueDate().isBefore(today)) {
                rent.setStatus(RentPaymentStatus.OVERDUE);
            }

            long totalDue = rent.getTotalDueInr() != null ? rent.getTotalDueInr() : 0L;
            long paid = rent.getPaidAmountInr() != null ? rent.getPaidAmountInr() : 0L;
            long arrears = Math.max(0, totalDue - paid);

            String msg;
            if (rent.getStatus() == RentPaymentStatus.OVERDUE) {
                msg = String.format(
                    "⚠️ *URGENT: Rent Payment Overdue*\n\n" +
                    "• Month: %s\n" +
                    "• Property ID: #%d\n" +
                    "• Total Arrears: *₹%d*\n" +
                    "• Due Date was: %s\n\n" +
                    "Please clear the pending balance immediately to maintain your Verified Tenant Trust Score.",
                    rent.getBillingCycleMonth(),
                    contract.getPropertyId(),
                    arrears,
                    rent.getDueDate()
                );
            } else {
                msg = String.format(
                    "📅 *Rent Due Reminder*\n\n" +
                    "• Month: %s\n" +
                    "• Total Payable: *₹%d*\n" +
                    "• Due Date: %s (in 2 days)\n\n" +
                    "Pay via UPI/Bank transfer and notify the property owner to avoid late fees.",
                    rent.getBillingCycleMonth(),
                    totalDue,
                    rent.getDueDate()
                );
            }

            if (contract.getTenantPhone() != null && !contract.getTenantPhone().isBlank()) {
                whatsAppSender.sendTextMessage(contract.getTenantPhone(), msg);
            }

            int count = rent.getReminderCount() != null ? rent.getReminderCount() : 0;
            rent.setReminderCount(count + 1);
            rent.setLastReminderSentAt(Instant.now());
            rentRepository.save(rent);
        }
    }

    private void sendRentDueWhatsAppNotice(RentSchedule rent, TenancyContract contract) {
        if (contract.getTenantPhone() == null || contract.getTenantPhone().isBlank()) return;

        // 🔍 Resolve Owner / Broker's active default UPI handle dynamically
        String brokerUpiId = upiHandleRepository.findByAccountIdAndIsDefaultTrue(contract.getOwnerAccountId())
                .map(com.risingbee.realestate.automation.domain.BrokerUpiHandle::getUpiId)
                .orElseGet(() -> {
                    var handles = upiHandleRepository.findByAccountId(contract.getOwnerAccountId());
                    if (handles != null && !handles.isEmpty()) {
                        return handles.get(0).getUpiId();
                    }
                    return "propertyhub@upi"; // Fallback handle
                });

        long totalDue = rent.getTotalDueInr() != null ? rent.getTotalDueInr() : 0L;

        // Construct dynamic UPI intent link
        String upiDeepLink = String.format("upi://pay?pa=%s&pn=PropertyHub&am=%d&cu=INR&tn=RentFor%s", 
                brokerUpiId, totalDue, rent.getBillingCycleMonth().replaceAll("\\s+", ""));

        String msg = String.format(
            "🏠 *Monthly Rent & Utility Invoice Generated*\n\n" +
            "• Billing Cycle: *%s*\n" +
            "• Base Rent: ₹%d\n" +
            "• Electricity: ₹%d\n" +
            "• Maintenance: ₹%d\n" +
            "• Water: ₹%d\n" +
            "──────────────\n" +
            "💰 *Total Amount Due: ₹%d*\n" +
            "📅 *Due Date: %s*\n\n" +
            "📲 *Pay Instantly via UPI:* \n%s\n\n" +
            "Please clear this invoice before the due date and share your screenshot/reference on the portal.",
            rent.getBillingCycleMonth(),
            rent.getBaseRentInr() != null ? rent.getBaseRentInr() : 0L,
            rent.getElectricityInr() != null ? rent.getElectricityInr() : 0L,
            rent.getMaintenanceInr() != null ? rent.getMaintenanceInr() : 0L,
            rent.getWaterInr() != null ? rent.getWaterInr() : 0L,
            totalDue,
            rent.getDueDate(),
            upiDeepLink
        );

        whatsAppSender.sendTextMessage(contract.getTenantPhone(), msg);
        log.info("Sent rent due WhatsApp notice with dynamic UPI handle={} for schedule #{}", brokerUpiId, rent.getId());
    }

    private void sendPaymentReceiptNotices(TenancyContract contract, RentSchedule schedule, Long amountPaid, String paymentMode, String txnRef) {
        String tenantPhone = contract.getTenantPhone();
        String safeMode = (paymentMode != null && !paymentMode.isBlank()) ? paymentMode : "DIRECT_PAY";
        String safeTxn = (txnRef != null && !txnRef.isBlank()) ? txnRef : "N/A";
        long safeAmount = (amountPaid != null) ? amountPaid : 0L;
        String cycle = (schedule.getBillingCycleMonth() != null) ? schedule.getBillingCycleMonth() : "Current Cycle";

        // 1. WhatsApp Receipt to Tenant
        if (tenantPhone != null && !tenantPhone.isBlank()) {
            String msg = String.format(
                "✅ *Rent Payment Confirmed!*\n\n" +
                "• Property: *Property #%d*\n" +
                "• Billing Cycle: *%s*\n" +
                "• Amount Paid: *₹%d*\n" +
                "• Payment Mode: *%s*\n" +
                "• Transaction Ref: *%s*\n" +
                "• Status: *PAID*\n\n" +
                "Thank you! Your rent ledger has been updated.",
                contract.getPropertyId(),
                cycle,
                safeAmount,
                safeMode,
                safeTxn
            );
            whatsAppSender.sendTextMessage(tenantPhone, msg);
        }

        // 2. WhatsApp Notification to Owner / Broker
        String ownerPhone = contract.getOwnerPhone();
        if (ownerPhone != null && !ownerPhone.isBlank() && !ownerPhone.equals(tenantPhone)) {
            String ownerMsg = String.format(
                "💰 *Rent Received Alert*\n\n" +
                "• Property: *Property #%d*\n" +
                "• Tenant: *%s* (+%s)\n" +
                "• Billing Cycle: *%s*\n" +
                "• Amount Collected: *₹%d* via %s\n" +
                "• Transaction Ref: *%s*",
                contract.getPropertyId(),
                contract.getTenantName() != null ? contract.getTenantName() : "Tenant",
                contract.getTenantPhone(),
                cycle,
                safeAmount,
                safeMode,
                safeTxn
            );
            whatsAppSender.sendTextMessage(ownerPhone, ownerMsg);
        }
    }

    public String normalizeCycle(String input) {
        if (input == null || input.isBlank()) {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
        }

        String cleaned = input.trim();

        // Standard HTML5 <input type="month"> format: YYYY-MM
        if (cleaned.matches("^\\d{4}-\\d{2}$")) {
            try {
                YearMonth ym = YearMonth.parse(cleaned);
                return ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
            } catch (Exception ignored) {}
        }

        // Parse human-typed variations: "sep 2026", "sept 2026", "09-2026", "September 2026"
        DateTimeFormatter flexibleParser = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("[MMMM yyyy][MMM yyyy][MM-yyyy][yyyy-MM]")
                .toFormatter(Locale.ENGLISH);

        try {
            YearMonth ym = YearMonth.parse(cleaned, flexibleParser);
            return ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
        } catch (Exception e) {
            log.warn("Could not parse month input [{}], falling back to raw string", cleaned);
            return cleaned;
        }
    }
}