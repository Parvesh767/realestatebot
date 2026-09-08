package com.risingbee.realestate.automation.utility.service;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.exception.ResourceNotFoundException;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;
import com.risingbee.realestate.automation.utility.domain.UtilityBill;
import com.risingbee.realestate.automation.utility.repo.UtilityBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UtilityBillingService {

    private final UtilityBillRepository billRepository;
    private final TenancyContractRepository contractRepository;
    private final WhatsAppSender whatsAppSender;

    public UtilityBill generateMonthlyBill(
            Actor actor,
            Long contractId,
            String billingMonth,
            Double previousReading,
            Double currentReading,
            Double ratePerUnit,
            Long fixedMaintenance,
            Long fixedWater
    ) {
        TenancyContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        if (currentReading < previousReading) {
            throw new IllegalArgumentException("Current reading cannot be lower than previous baseline reading.");
        }

        double units = currentReading - previousReading;
        long electricityCost = Math.round(units * (ratePerUnit != null ? ratePerUnit : 8.5));
        long maintenance = fixedMaintenance != null ? fixedMaintenance : 0L;
        long water = fixedWater != null ? fixedWater : 0L;
        long total = electricityCost + maintenance + water;

        UtilityBill bill = new UtilityBill();
        bill.setContract(contract);
        bill.setBillingMonth(billingMonth);
        bill.setPreviousMeterReading(previousReading);
        bill.setCurrentMeterReading(currentReading);
        bill.setUnitsConsumed(units);
        bill.setRatePerUnit(ratePerUnit != null ? ratePerUnit : 8.5);
        bill.setElectricityAmountInr(electricityCost);
        bill.setMaintenanceChargeInr(maintenance);
        bill.setWaterChargeInr(water);
        bill.setTotalPayableInr(total);

        billRepository.save(bill);

        // Auto-dispatch itemized rent + utility WhatsApp invoice to Tenant
        dispatchWhatsAppInvoice(bill, contract);

        return bill;
    }

    private void dispatchWhatsAppInvoice(UtilityBill bill, TenancyContract contract) {
        long rent = contract.getMonthlyRent() != null ? contract.getMonthlyRent() : 0L;
        long grandTotal = rent + bill.getTotalPayableInr();

        String invoice = String.format(
            "📄 *Monthly Rent & Utility Invoice — %s*\n\n" +
            "• Apartment / Property ID: #%d\n" +
            "• Base Monthly Rent: *₹%d*\n\n" +
            "⚡ *Sub-Meter Electricity Breakdown:*\n" +
            "  - Previous: %.1f | Current: %.1f\n" +
            "  - Units: *%.1f Units* @ ₹%.1f/Unit\n" +
            "  - Electricity Total: *₹%d*\n" +
            "  - Maintenance / Society: ₹%d\n" +
            "  - Water Charges: ₹%d\n\n" +
            "💰 *Total Payable for %s: ₹%d*\n\n" +
            "Please clear payment by the 5th of the month via bank transfer/UPI.",
            bill.getBillingMonth(), contract.getPropertyId(), rent,
            bill.getPreviousMeterReading(), bill.getCurrentMeterReading(),
            bill.getUnitsConsumed(), bill.getRatePerUnit(),
            bill.getElectricityAmountInr(), bill.getMaintenanceChargeInr(),
            bill.getWaterChargeInr(), bill.getBillingMonth(), grandTotal
        );

        whatsAppSender.sendTextMessage(contract.getTenantPhone(), invoice);
    }
}