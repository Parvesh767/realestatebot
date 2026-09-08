package com.risingbee.realestate.automation.tenancy.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.domain.BrokerUpiHandle;
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
public class TenancyPaymentService {

	private final RentScheduleRepository rentScheduleRepository;
	private final TenancyContractRepository contractRepository;
	private final WhatsAppSender whatsAppSender;
	private final AccountRepository accountRepository;
	private final BrokerUpiHandleRepository upiRepository;
	

	public void sendPaymentReminder(Long contractId, Long scheduleId) {
		TenancyContract contract = contractRepository.findById(contractId)
				.orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

		RentSchedule schedule = rentScheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new IllegalArgumentException("Rent schedule not found: " + scheduleId));

		long totalDue = schedule.getTotalDueInr() - schedule.getPaidAmountInr();
		if (totalDue <= 0) {
			throw new IllegalStateException("No pending balance for this billing cycle.");
		}

		String tenantPhone = contract.getTenantPhone();
		if (tenantPhone == null || tenantPhone.isBlank()) {
			throw new IllegalStateException("Tenant phone number is missing.");
		}

		// 🔍 Resolve Owner / Broker Account dynamic default UPI handle
		String brokerUpiId = upiRepository.findByAccountIdAndIsDefaultTrue(contract.getOwnerAccountId())
				.map(com.risingbee.realestate.automation.domain.BrokerUpiHandle::getUpiId)
				.orElseGet(() -> {
					var handles = upiRepository.findByAccountId(contract.getOwnerAccountId());
					if (handles != null && !handles.isEmpty()) {
						return handles.get(0).getUpiId();
					}
					return "propertyhub@upi"; // Fallback VPA
				});

		// Construct dynamic UPI intent link
		String upiDeepLink = String.format("upi://pay?pa=%s&pn=PropertyHub&am=%d&cu=INR&tn=RentFor%s", brokerUpiId,
				totalDue, schedule.getBillingCycleMonth().replaceAll("\\s+", ""));

		String message = """
				🔔 *Rent Payment Reminder*

				Dear %s,
				This is a friendly reminder that your rent for *%s* is due.

				• *Base Rent:* ₹%d
				• *Utilities / Charges:* ₹%d
				• *Total Balance Due:* *₹%d*
				• *Due Date:* %s

				📲 *Pay instantly via UPI:*
				%s

				Please share the screenshot or transaction reference once paid. Thank you!
				""".formatted(contract.getTenantName(), schedule.getBillingCycleMonth(), schedule.getBaseRentInr(),
				(schedule.getElectricityInr() + schedule.getMaintenanceInr() + schedule.getWaterInr()), totalDue,
				schedule.getDueDate(), upiDeepLink);

		whatsAppSender.sendTextMessage(tenantPhone, message);
		log.info("Sent dynamic WhatsApp payment reminder for contract #{} schedule #{} using UPI handle={}", contractId,
				scheduleId, brokerUpiId);
	}

	/**
	 * Helper to resolve the owner/broker's active UPI handle. Fallback strategy:
	 * uses their registered phone number linked with standard handle (@okaxis
	 * / @paytm), or a custom UPI field if you add one to your Account entity.
	 */
	private String resolveBrokerUpi(Long ownerAccountId) {
	    if (ownerAccountId == null) return "propertyhub@upi";

	    return upiRepository.findByAccountIdAndIsDefaultTrue(ownerAccountId)
	            .map(BrokerUpiHandle::getUpiId)
	            .orElseGet(() -> {
	                // Fallback to first available handle or phone default
	                List<BrokerUpiHandle> handles = upiRepository.findByAccountId(ownerAccountId);
	                if (!handles.isEmpty()) return handles.get(0).getUpiId();
	                return "propertyhub@upi";
	            });
	}
}