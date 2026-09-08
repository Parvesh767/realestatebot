package com.risingbee.realestate.automation.payment.service;

import com.risingbee.realestate.automation.domain.BrokerUpiHandle;
import com.risingbee.realestate.automation.rent.domain.RentPaymentStatus;
import com.risingbee.realestate.automation.rent.domain.RentSchedule;
import com.risingbee.realestate.automation.rent.repo.RentScheduleRepository;
import com.risingbee.realestate.automation.repo.BrokerUpiHandleRepository;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedPaymentService {

    private final RentScheduleRepository rentRepository;
    private final TenancyContractRepository contractRepository;
    private final BrokerUpiHandleRepository upiHandleRepository;

    /**
     * Resolves the active/default UPI ID for the property owner/broker associated with a contract.
     */
    public String resolveActiveUpi(Long contractId) {
        TenancyContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        if (contract.getOwnerAccountId() == null) {
            return "propertyhub@upi";
        }

        // Look up the broker's saved default UPI handle, fallback to first handle or generic VPA
        return upiHandleRepository.findByAccountIdAndIsDefaultTrue(contract.getOwnerAccountId())
                .map(BrokerUpiHandle::getUpiId)
                .orElseGet(() -> {
                    List<BrokerUpiHandle> handles = upiHandleRepository.findByAccountId(contract.getOwnerAccountId());
                    if (!handles.isEmpty()) {
                        return handles.get(0).getUpiId();
                    }
                    return "propertyhub@upi";
                });
    }

    /**
     * Unified processor for marking a rent schedule as PAID via any mode (Gateway, UPI Intent, or Manual Verification).
     */
    @Transactional
    public RentSchedule processSuccessfulPayment(Long scheduleId, String paymentMode, String transactionReference, Long amountPaid) {
        RentSchedule schedule = rentRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Rent schedule not found: " + scheduleId));

        schedule.setStatus(RentPaymentStatus.PAID);
        schedule.setPaidAmountInr(amountPaid != null ? amountPaid : schedule.getTotalDueInr());
        schedule.setPaidAt(Instant.now());
        schedule.setPaymentMode(paymentMode != null ? paymentMode : "UNIFIED_UPI");
        schedule.setTransactionReference(transactionReference);

        RentSchedule saved = rentRepository.save(schedule);
        log.info("UnifiedPayment: Successfully processed payment for Rent Schedule #{} via {} with ref={}", 
                scheduleId, paymentMode, transactionReference);
        
        return saved;
    }
}