package com.risingbee.realestate.automation.service_hub.service;

import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.automation.service_hub.domain.ServiceBooking;
import com.risingbee.realestate.automation.service_hub.domain.ServiceVendor;
import com.risingbee.realestate.automation.service_hub.enums.BookingStatus;
import com.risingbee.realestate.automation.service_hub.repo.ServiceBookingRepository;
import com.risingbee.realestate.automation.service_hub.repo.ServiceVendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VendorWhatsAppDispatcher {

    private final ServiceBookingRepository bookingRepository;
    private final ServiceVendorRepository vendorRepository;
    private final ServiceHubService serviceHubService;
    private final WhatsAppSender whatsAppSender;

    /**
     * Inbound WhatsApp Command Handler for Service Vendors
     * Commands:
     *   "ACCEPT <bookingId>"
     *   "OTP <bookingId> <otp>"
     *   "BALANCE"
     */
    public boolean handleVendorInboundMessage(String fromPhone, String rawText) {
        String cleanPhone = fromPhone.replaceAll("[^0-9]", "");
        Optional<ServiceVendor> vendorOpt = vendorRepository.findByPhone(cleanPhone);

        if (vendorOpt.isEmpty()) {
            return false; // Message is from a standard client, not a technician
        }

        ServiceVendor vendor = vendorOpt.get();
        String text = rawText.trim().toUpperCase();

        if (text.startsWith("ACCEPT ")) {
            handleAcceptJob(vendor, text.replace("ACCEPT ", "").trim());
            return true;
        } else if (text.startsWith("OTP ")) {
            handleOtpSubmission(vendor, text.replace("OTP ", "").trim());
            return true;
        } else if ("BALANCE".equals(text) || "PAYOUT".equals(text)) {
            sendBalanceSummary(vendor);
            return true;
        }

        sendHelpMenu(vendor);
        return true;
    }

    private void handleAcceptJob(ServiceVendor vendor, String bookingIdStr) {
        try {
            Long bookingId = Long.parseLong(bookingIdStr);
            ServiceBooking booking = bookingRepository.findById(bookingId).orElse(null);

            if (booking == null || !BookingStatus.ASSIGNED.equals(booking.getStatus())) {
                whatsAppSender.sendTextMessage(vendor.getPhone(), "❌ Job #" + bookingId + " is no longer available or already active.");
                return;
            }

            booking.setStatus(BookingStatus.IN_PROGRESS);
            bookingRepository.save(booking);

            String reply = String.format(
                "✅ *Job #%d Accepted!*\n\n" +
                "• Customer Phone: +%s\n" +
                "• Address: %s\n" +
                "• Guaranteed Payout: ₹%d\n\n" +
                "After finishing work, ask customer for the 4-digit OTP and text:\n" +
                "*OTP %d <4-digit-code>*",
                booking.getId(), booking.getUserPhone(), booking.getUserAddress(),
                booking.getVendorPayoutAmount(), booking.getId()
            );
            whatsAppSender.sendTextMessage(vendor.getPhone(), reply);

        } catch (NumberFormatException e) {
            whatsAppSender.sendTextMessage(vendor.getPhone(), "❌ Invalid syntax. Use: *ACCEPT <BookingID>*");
        }
    }

    private void handleOtpSubmission(ServiceVendor vendor, String payload) {
        try {
            String[] parts = payload.split("\\s+");
            if (parts.length < 2) {
                whatsAppSender.sendTextMessage(vendor.getPhone(), "❌ Invalid format. Use: *OTP <BookingID> <4-Digit-OTP>* (e.g., OTP 7 4437)");
                return;
            }

            Long bookingId = Long.parseLong(parts[0]);
            String otp = parts[1];

            serviceHubService.completeJobWithOtp(bookingId, otp);

        } catch (Exception e) {
            whatsAppSender.sendTextMessage(vendor.getPhone(), "❌ Error: " + e.getMessage());
        }
    }

    private void sendBalanceSummary(ServiceVendor vendor) {
        String msg = String.format(
            "💼 *Vendor Payout Dashboard*\n\n" +
            "• Technician: %s\n" +
            "• Completed Jobs: %d\n" +
            "• Pending Bank Balance: *₹%d*\n" +
            "• Total Paid Out: ₹%d\n" +
            "• Registered UPI: %s\n\n" +
            "Payouts process weekly directly to your UPI ID.",
            vendor.getName(), vendor.getCompletedJobsCount(),
            vendor.getPendingPayoutBalance() != null ? vendor.getPendingPayoutBalance() : 0,
            vendor.getTotalPaidOut() != null ? vendor.getTotalPaidOut() : 0,
            vendor.getUpiId() != null ? vendor.getUpiId() : "Not registered"
        );
        whatsAppSender.sendTextMessage(vendor.getPhone(), msg);
    }

    private void sendHelpMenu(ServiceVendor vendor) {
        String menu = "🔧 *Partner Commands:*\n\n" +
                      "• *ACCEPT <ID>* — Confirm job dispatch\n" +
                      "• *OTP <ID> <CODE>* — Finalize completion\n" +
                      "• *BALANCE* — View accrued earnings";
        whatsAppSender.sendTextMessage(vendor.getPhone(), menu);
    }
}