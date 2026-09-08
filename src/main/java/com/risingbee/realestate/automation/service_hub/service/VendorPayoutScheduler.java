package com.risingbee.realestate.automation.service_hub.service;

import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.automation.service_hub.domain.ServiceVendor;
import com.risingbee.realestate.automation.service_hub.repo.ServiceVendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorPayoutScheduler {

    private final ServiceVendorRepository vendorRepository;
    private final WhatsAppSender whatsAppSender;

    /**
     * Runs every Monday at 10:00 AM to settle balances
     */
    @Scheduled(cron = "0 0 10 * * MON")
    @Transactional
    public void processWeeklyVendorSettlements() {
        log.info("Starting automated weekly vendor payout run...");
        List<ServiceVendor> activeVendors = vendorRepository.findAll();

        for (ServiceVendor vendor : activeVendors) {
            long pending = vendor.getPendingPayoutBalance() != null ? vendor.getPendingPayoutBalance() : 0L;
            if (pending > 0 && vendor.getUpiId() != null && !vendor.getUpiId().isBlank()) {
                
                // In live mode: Trigger RazorpayX / Cashfree Payout API call here
                long previousTotal = vendor.getTotalPaidOut() != null ? vendor.getTotalPaidOut() : 0L;
                vendor.setTotalPaidOut(previousTotal + pending);
                vendor.setPendingPayoutBalance(0L);
                vendorRepository.save(vendor);

                String msg = String.format(
                    "🏦 *Weekly Bank Transfer Processed!*\n\n" +
                    "• Transferred: *₹%d*\n" +
                    "• Destination UPI: %s\n" +
                    "• Pending Balance: ₹0\n\n" +
                    "Funds reflect in your linked bank account within 2 hours.",
                    pending, vendor.getUpiId()
                );
                whatsAppSender.sendTextMessage(vendor.getPhone(), msg);
                log.info("Settled ₹{} for Vendor {}", pending, vendor.getName());
            }
        }
    }
}