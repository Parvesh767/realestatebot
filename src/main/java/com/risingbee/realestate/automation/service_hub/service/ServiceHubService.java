package com.risingbee.realestate.automation.service_hub.service;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.exception.ResourceNotFoundException;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.automation.service_hub.domain.ServiceBooking;
import com.risingbee.realestate.automation.service_hub.domain.ServiceCatalog;
import com.risingbee.realestate.automation.service_hub.domain.ServiceVendor;
import com.risingbee.realestate.automation.service_hub.dto.ServiceBookingRequestDTO;
import com.risingbee.realestate.automation.service_hub.enums.BookingStatus;
import com.risingbee.realestate.automation.service_hub.repo.ServiceBookingRepository;
import com.risingbee.realestate.automation.service_hub.repo.ServiceCatalogRepository;
import com.risingbee.realestate.automation.service_hub.repo.ServiceVendorRepository;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.enums.TicketStatus;
import com.risingbee.realestate.automation.tenancy.repo.MaintenanceTicketRepository;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;
import com.risingbee.realestate.automation.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ServiceHubService {

    private final ServiceCatalogRepository catalogRepository;
    private final ServiceBookingRepository bookingRepository;
    private final ServiceVendorRepository vendorRepository;
    private final AccountRepository accountRepository;
    private final TenancyContractRepository contractRepository;
    private final WhatsAppSender whatsAppSender;
    private final MaintenanceTicketRepository ticketRepository;

    private static final long FIXED_CREDIT_VALUE_INR = 30L;

    public ServiceBooking createBooking(Actor actor, ServiceBookingRequestDTO req) {
        Long accountId = actor.internalId();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        ServiceCatalog item = catalogRepository.findById(req.getServiceCatalogId())
                .orElseThrow(() -> new ResourceNotFoundException("Service item not found"));

        ServiceBooking booking = new ServiceBooking();
        booking.setAccountId(accountId);
        booking.setBookedByRole(actor.role());

        String buyerPhone = PhoneUtils.formatWhatsAppNumber(
                account.getPhone() != null ? account.getPhone() : account.getExternalId()
        );
        booking.setUserPhone(buyerPhone);
        booking.setUserAddress(req.getAddress());
        booking.setLandmark(req.getLandmark());
        booking.setLocalityCode(req.getLocalityCode());
        booking.setServiceItem(item);
        booking.setCategory(item.getCategory());
        booking.setIssueDescription(req.getIssueDescription());
        booking.setPreferredDate(req.getPreferredDate());
        booking.setPreferredTimeSlot(req.getPreferredTimeSlot());

        
        
        if (req.getMaintenanceTicketId() != null) {
            ticketRepository.findById(req.getMaintenanceTicketId()).ifPresent(ticket -> {
                ticket.setStatus(TicketStatus.ASSIGNED);
                ticketRepository.save(ticket);
            });
        }
        
        // Tenancy Linkage & Determining OTP Recipient
        String otpRecipientPhone = buyerPhone;
        if (req.getTenancyContractId() != null) {
            TenancyContract contract = contractRepository.findById(req.getTenancyContractId()).orElse(null);
            if (contract != null) {
                booking.setTenancyContractId(contract.getId());
                booking.setPropertyId(contract.getPropertyId());
                if (contract.getTenantPhone() != null && !contract.getTenantPhone().isBlank()) {
                    otpRecipientPhone = PhoneUtils.formatWhatsAppNumber(contract.getTenantPhone());
                }
            }
        }
        
        
        
        booking.setTargetRecipientPhone(otpRecipientPhone);

        // Generate 4-digit OTP
        String otp = String.format("%04d", new SecureRandom().nextInt(10000));
        booking.setCompletionOtp(otp);

        // Calculations & Payment Logic
        long vendorCost = item.getVendorPayoutInr() != null ? item.getVendorPayoutInr() : 180L;
        booking.setVendorPayoutAmount(vendorCost);

        if ("CREDIT".equalsIgnoreCase(req.getPaymentMode())) {
            int neededCredits = item.getCreditCost();
            int currentCredits = account.getCreditsBalance() != null ? account.getCreditsBalance() : 0;

            if (currentCredits < neededCredits) {
                throw new IllegalStateException("Insufficient credits (" + currentCredits + " available, " + neededCredits + " required).");
            }

            account.setCreditsBalance(currentCredits - neededCredits);
            accountRepository.save(account);

            booking.setCreditsDeducted(neededCredits);
            booking.setPaymentMode("CREDIT");

            long grossCollected = (long) neededCredits * FIXED_CREDIT_VALUE_INR;
            booking.setPlatformGrossMargin(Math.max(0, grossCollected - vendorCost));
        } else {
            booking.setPaymentMode("PAY_AFTER_SERVICE");
            booking.setAmountPaidInr(item.getFiatPriceInr());
            booking.setPlatformGrossMargin(Math.max(0, item.getFiatPriceInr() - vendorCost));
        }

        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);

        // Broadcast the job opportunity to nearby qualified vendors
        broadcastJobToVendors(booking, item);


        // If booked by Broker on behalf of Tenant, send receipt without OTP
        if (!buyerPhone.equals(otpRecipientPhone)) {
            notifyBrokerBookingDispatched(booking, buyerPhone, otpRecipientPhone, item);
        }

        log.info("Service #{} created in PENDING state and broadcasted to vendors.", booking.getId());
        return booking;
    }

    public ServiceBooking completeJobWithOtp(Long bookingId, String enteredOtp) {
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Job #" + bookingId + " has already been marked as completed.");
        }
        
        if (!booking.getCompletionOtp().equals(enteredOtp.trim())) {
            throw new IllegalArgumentException("Invalid completion OTP. Please verify with the on-site occupant.");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());

        if (booking.getAssignedVendor() != null) {
            ServiceVendor vendor = booking.getAssignedVendor();
            vendor.setCompletedJobsCount(vendor.getCompletedJobsCount() + 1);

            long currentBalance = vendor.getPendingPayoutBalance() != null ? vendor.getPendingPayoutBalance() : 0L;
            vendor.setPendingPayoutBalance(currentBalance + booking.getVendorPayoutAmount());
            vendorRepository.save(vendor);

            String techMsg = String.format(
                "💰 *Job Payment Credited!*\n\n" +
                "• Job: #%d (%s)\n" +
                "• Payout: *₹%d*\n" +
                "• Updated Balance: *₹%d*\n\n" +
                "Payouts settle weekly via registered UPI.",
                booking.getId(), booking.getServiceItem().getTitle(),
                booking.getVendorPayoutAmount(), vendor.getPendingPayoutBalance()
            );
            whatsAppSender.sendTextMessage(PhoneUtils.formatWhatsAppNumber(vendor.getPhone()), techMsg);
        }

        bookingRepository.save(booking);

        // Notify on-site occupant
        String targetMsg = String.format("✅ *Service Completed!* Job #%d is closed. Thank you for choosing Property Hub.", booking.getId());
        whatsAppSender.sendTextMessage(PhoneUtils.formatWhatsAppNumber(booking.getTargetRecipientPhone()), targetMsg);

        return booking;
    }

    private void notifyRecipientOnWhatsApp(ServiceBooking booking, String recipientPhone, ServiceCatalog item) {
        String msg = String.format(
            "🛠️ *Home Service Scheduled!*\n\n" +
            "• Service: *%s*\n" +
            "• Date & Slot: %s (%s)\n" +
            "• Address: %s\n\n" +
            "🔑 *Completion OTP: %s*\n\n" +
            "Please inspect the technician's work upon arrival and share this OTP *only* after the service is successfully completed.",
            item.getTitle(), booking.getPreferredDate(), booking.getPreferredTimeSlot(),
            booking.getUserAddress(), booking.getCompletionOtp()
        );
        whatsAppSender.sendTextMessage(recipientPhone, msg);
    }

    private void notifyBrokerBookingDispatched(ServiceBooking booking, String brokerPhone, String tenantPhone, ServiceCatalog item) {
        String msg = String.format(
            "📋 *Service Booking Confirmed for Tenant*\n\n" +
            "• Service: *%s*\n" +
            "• Scheduled: %s (%s)\n" +
            "• Tenant Mobile: +%s\n" +
            "• Address: %s\n\n" +
            "The 4-digit Completion OTP has been sent directly to the tenant's WhatsApp and Tenant Portal.",
            item.getTitle(), booking.getPreferredDate(), booking.getPreferredTimeSlot(),
            tenantPhone, booking.getUserAddress()
        );
        whatsAppSender.sendTextMessage(brokerPhone, msg);
    }

    private void notifyVendorOnWhatsApp(ServiceBooking booking, ServiceVendor vendor, ServiceCatalog item) {
        String msg = String.format(
            "🔧 *New Job Assigned*\n\n" +
            "• Task: *%s*\n" +
            "• Slot: %s (%s)\n" +
            "• Location: %s\n" +
            "• Contact Person: +%s\n" +
            "• Guaranteed Payout: *₹%d*\n\n" +
            "Collect the 4-digit completion OTP from the on-site occupant once the work is verified.",
            item.getTitle(), booking.getPreferredDate(), booking.getPreferredTimeSlot(),
            booking.getUserAddress(), booking.getTargetRecipientPhone(), booking.getVendorPayoutAmount()
        );
        whatsAppSender.sendTextMessage(PhoneUtils.formatWhatsAppNumber(vendor.getPhone()), msg);
    }
    
    
    /**
     * Broadcasts job opportunity to up to 3 nearby active technicians
     */
    public void broadcastJobToVendors(ServiceBooking booking, ServiceCatalog item) {
        List<ServiceVendor> eligibleVendors = vendorRepository
                .findBySkillCategoryAndOperationalCityCodeAndActiveTrue(item.getCategory(), booking.getLocalityCode());

        if (eligibleVendors.isEmpty()) {
            // Fallback to city-wide vendors if locality has no active staff
            eligibleVendors = vendorRepository
                    .findBySkillCategoryAndOperationalCityCodeAndActiveTrue(item.getCategory(), "GUR");
        }

        // Limit broadcast to maximum 3 nearby vendors to prevent spam
        List<ServiceVendor> targets = eligibleVendors.stream().limit(3).toList();

        for (ServiceVendor vendor : targets) {
            String broadcastMsg = String.format(
                "⚡ *New Service Job Available!* (Booking #%d)\n\n" +
                "• Task: *%s*\n" +
                "• Date & Slot: %s (%s)\n" +
                "• Locality: *%s*\n" +
                "• Guaranteed Payout: *₹%d*\n\n" +
                "👉 Reply with: *ACCEPT %d*",
                booking.getId(), item.getTitle(), booking.getPreferredDate(), booking.getPreferredTimeSlot(),
                booking.getLocalityCode(), booking.getVendorPayoutAmount(), booking.getId()
            );
            whatsAppSender.sendTextMessage(PhoneUtils.formatWhatsAppNumber(vendor.getPhone()), broadcastMsg);
        }
        log.info("Broadcasted Booking #{} to {} vendors", booking.getId(), targets.size());
    }

    /**
     * First-claim lock when technician sends "ACCEPT <ID>"
     */
    public synchronized boolean acceptJob(String vendorPhone, Long bookingId) {
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        // Ensure race conditions are blocked if another tech claimed it first
        if (booking.getStatus() != BookingStatus.PENDING) {
            whatsAppSender.sendTextMessage(vendorPhone, "⚠️ Job #" + bookingId + " has already been claimed by another partner.");
            return false;
        }

        // Normalize phone variations (918847084526 vs 8847084526)
        String rawPhone = vendorPhone.replaceAll("[^0-9]", "");
        String strippedPhone = rawPhone.startsWith("91") && rawPhone.length() == 12 ? rawPhone.substring(2) : rawPhone;

        List<ServiceVendor> matchingVendors = vendorRepository.findAllByPhone(rawPhone);
        if (matchingVendors.isEmpty()) {
            matchingVendors = vendorRepository.findAllByPhone(strippedPhone);
        }
        
        if (matchingVendors.isEmpty()) {
            matchingVendors = vendorRepository.findAllByPhone("+" + rawPhone);
        }

        if (matchingVendors.isEmpty()) {
            throw new ResourceNotFoundException("Vendor not registered for phone: " + vendorPhone);
        }
        
        
     // Match the vendor profile that matches the booking's required category/skill (e.g., CARPENTRY, ELECTRICIAN)
        ServiceVendor vendor = matchingVendors.stream()
                .filter(v -> v.isActive() && v.getSkillCategory().name().equalsIgnoreCase(booking.getCategory().name()))
                .findFirst()
                .orElse(matchingVendors.get(0)); // Fallback to first profile if skill doesn't strictly match
        
        
        
        // Lock assignment
        booking.setAssignedVendor(vendor);
        booking.setStatus(BookingStatus.ASSIGNED);
        bookingRepository.save(booking);

        if (booking.getMaintenanceTicketId() != null) {
            ticketRepository.findById(booking.getMaintenanceTicketId()).ifPresent(t -> {
                t.setStatus(TicketStatus.ASSIGNED);
                ticketRepository.save(t);
            });
        }

        // Send confirmation to Technician with customer details
        String techConfirmation = String.format(
        	    "✅ *Job #%d Confirmed!*\n\n" +
        	    "• Task Category: *%s*\n" +
        	    "• Customer Address: %s\n" +
        	    "• Contact Person: +%s\n" +
        	    "• Slot: %s (%s)\n\n" +
        	    "📍 *To complete this job once finished, reply with:*\n" +
        	    "👉 *COMPLETE %d <4-digit PIN>*",
        	    booking.getId(), booking.getCategory(), booking.getUserAddress(), booking.getTargetRecipientPhone(),
        	    booking.getPreferredDate(), booking.getPreferredTimeSlot(), booking.getId()
        	);
        whatsAppSender.sendTextMessage(vendorPhone, techConfirmation);

        // Send Completion OTP to Tenant/Occupant
        String tenantAlert = String.format(
            "🛠️ *Technician Assigned!*\n\n" +
            "• Technician: *%s* (+%s)\n" +
            "• Slot: %s (%s)\n" +
            "• Completion PIN: *%s*\n\n" +
            "Share this PIN only after verifying the work.",
            vendor.getName(), vendor.getPhone(),
            booking.getPreferredDate(), booking.getPreferredTimeSlot(),
            booking.getCompletionOtp()
        );
        whatsAppSender.sendTextMessage(booking.getTargetRecipientPhone(), tenantAlert);

        return true;
    }
    
    
    
}