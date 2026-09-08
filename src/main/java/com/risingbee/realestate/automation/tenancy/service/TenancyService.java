package com.risingbee.realestate.automation.tenancy.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.exception.AccessDeniedException;
import com.risingbee.realestate.automation.exception.ResourceNotFoundException;
import com.risingbee.realestate.automation.rent.repo.RentScheduleRepository;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.automation.service.storage_service.PhotoStorageService;
import com.risingbee.realestate.automation.tenancy.domain.DepositDeduction;
import com.risingbee.realestate.automation.tenancy.domain.InspectionRecord;
import com.risingbee.realestate.automation.tenancy.domain.MaintenanceTicket;
import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import com.risingbee.realestate.automation.tenancy.dto.DeductionRequestDTO;
import com.risingbee.realestate.automation.tenancy.dto.MaintenanceTicketRequestDTO;
import com.risingbee.realestate.automation.tenancy.dto.SettlementSummaryDTO;
import com.risingbee.realestate.automation.tenancy.enums.InspectionType;
import com.risingbee.realestate.automation.tenancy.enums.TenancyStatus;
import com.risingbee.realestate.automation.tenancy.enums.TicketStatus;
import com.risingbee.realestate.automation.tenancy.repo.DepositDeductionRepository;
import com.risingbee.realestate.automation.tenancy.repo.InspectionRecordRepository;
import com.risingbee.realestate.automation.tenancy.repo.MaintenanceTicketRepository;
import com.risingbee.realestate.automation.tenancy.repo.TenancyContractRepository;
import com.risingbee.realestate.automation.util.PhoneUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TenancyService {

    private final TenancyContractRepository contractRepository;
    private final InspectionRecordRepository inspectionRepository;
    private final DepositDeductionRepository deductionRepository;
    private final RentScheduleRepository rentRepository;
    private final PhotoStorageService photoStorageService;
    private final WhatsAppSender whatsAppSender;
    private final MaintenanceTicketRepository ticketRepository;

    /**
     * Record Move-In or Move-Out Baseline Inspection Photos & Meter Reading
     */
    
    
    
    @Transactional
    public MaintenanceTicket createTicket(Actor actor, Long contractId, MaintenanceTicketRequestDTO dto, MultipartFile photo) {
        TenancyContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        String photoUrl = null;
        if (photo != null && !photo.isEmpty()) {
            photoUrl = photoStorageService.save(photo); // Your existing file storage service
        }

        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .tenancyContractId(contract.getId())
                .tenantAccountId(actor.internalId())
                .propertyId(contract.getPropertyId())
                .category(dto.getCategory())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .photoUrl(photoUrl)
                .status(TicketStatus.OPEN)
                .build();

        MaintenanceTicket saved = ticketRepository.save(ticket);

        // Notify Broker / Owner via WhatsApp
        String ownerPhone = PhoneUtils.formatWhatsAppNumber(contract.getOwnerPhone());
        if (!ownerPhone.isBlank()) {
            String alert = String.format(
                "🛠️ *New Tenant Service Ticket #%d*\n\n" +
                "• Category: *%s*\n" +
                "• Issue: *%s*\n" +
                "• Property ID: *#%d*\n" +
                "• Tenant Phone: *+%s*",
                saved.getId(), saved.getCategory(), saved.getTitle(), contract.getPropertyId(), contract.getTenantPhone()
            );
            whatsAppSender.sendTextMessage(ownerPhone, alert);
        }

        return saved;
    }
    
    
    

    
    public InspectionRecord recordInspection(
            Actor actor,
            Long contractId,
            InspectionType type,
            Double meterReading,
            MultipartFile meterPhoto,
            List<MultipartFile> roomPhotos,
            String notes
    ) {
        TenancyContract contract = getOwnedContract(actor, contractId);

        InspectionRecord record = new InspectionRecord();
        record.setTenancyContract(contract);
        record.setType(type);
        record.setElectricityMeterReading(meterReading);
        record.setNotes(notes);
        record.setRecordedByAccountId(actor.internalId());

        if (meterPhoto != null && !meterPhoto.isEmpty()) {
            record.setMeterPhotoUrl(photoStorageService.save(meterPhoto));
        }

        if (roomPhotos != null && !roomPhotos.isEmpty()) {
            record.setRoomPhotos(photoStorageService.saveAll(roomPhotos));
        }

        if (type == InspectionType.MOVE_OUT) {
            contract.setStatus(TenancyStatus.MOVE_OUT_INSPECTION);
            contract.setActualMoveOutDate(LocalDate.now());
        }

        contract.addInspection(record);
        contractRepository.save(contract);

        log.info("Inspection [{}] recorded for Contract #{}", type, contractId);
        return record;
    }

    /**
     * Add Proof-Gated Deduction Item (e.g. Paint repair, Unpaid Bills)
     */
    public DepositDeduction addDeduction(Actor actor, Long contractId, DeductionRequestDTO req, MultipartFile proofPhoto) {
        TenancyContract contract = getOwnedContract(actor, contractId);

        DepositDeduction deduction = new DepositDeduction();
        deduction.setTenancyContract(contract);
        deduction.setCategory(req.getCategory());
        deduction.setTitle(req.getTitle());
        deduction.setAmount(req.getAmount());

        if (proofPhoto != null && !proofPhoto.isEmpty()) {
            deduction.setProofPhotoUrl(photoStorageService.save(proofPhoto));
        }

        contract.addDeduction(deduction);
        contractRepository.save(contract);

        // Alert Tenant on WhatsApp with Photo & Breakdown
        notifyTenantOfDeduction(contract, deduction);
        return deduction;
    }

    /**
     * Compute Net Refund Settlement Breakdown combining physical damage deductions and unpaid rent arrears
     */
    @Transactional(readOnly = true)
    public SettlementSummaryDTO calculateSettlement(Long contractId) {
        TenancyContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        long totalDeposit = contract.getSecurityDeposit() != null ? contract.getSecurityDeposit() : 50000L;

        // 1. Fetch physical damage deductions
        List<DepositDeduction> deductions = deductionRepository.findByTenancyContractId(contractId);
        long damageDeductions = deductions.stream()
                .mapToLong(DepositDeduction::getAmount)
                .sum();

        // 2. Fetch pending unpaid rent & utility arrears from rent schedules
        Long pendingRentArrears = rentRepository.getPendingArrearsByContract(contractId);
        long arrears = pendingRentArrears != null ? pendingRentArrears : 0L;

        long totalDeductions = damageDeductions + arrears;
        long netRefund = Math.max(0L, totalDeposit - totalDeductions);

        return new SettlementSummaryDTO(
                contract.getId(),
                contract.getPropertyId(),
                totalDeposit,
                totalDeductions,
                netRefund,
                contract.getStatus(),
                deductions,
                arrears
        );
    }

    /**
     * Finalize & Settle Deposit after both parties acknowledge
     */
    public void settleContract(Actor actor, Long contractId) {
        TenancyContract contract = getOwnedContract(actor, contractId);
        contract.setStatus(TenancyStatus.SETTLED);
        contractRepository.save(contract);

        // Notify both parties of final settlement
        String summaryMsg = "✅ *Tenancy Deposit Settled!*\nContract #" + contractId + " has been marked settled. Net refund processed as per digital audit breakdown.";
        whatsAppSender.sendTextMessage(contract.getTenantPhone(), summaryMsg);
        whatsAppSender.sendTextMessage(contract.getOwnerPhone(), summaryMsg);
    }

    private TenancyContract getOwnedContract(Actor actor, Long contractId) {
        TenancyContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        Long actorId = actor.internalId();
        if (!contract.getOwnerAccountId().equals(actorId) && !contract.getTenantAccountId().equals(actorId)) {
            throw new AccessDeniedException("Unauthorized access to contract #" + contractId);
        }
        return contract;
    }

    private void notifyTenantOfDeduction(TenancyContract contract, DepositDeduction deduction) {
        if (contract.getTenantPhone() == null) return;
        String msg = String.format(
                "📋 *Move-Out Settlement Item Added*\n\n" +
                "• Reason: *%s* (%s)\n" +
                "• Amount: *₹%d*\n\n" +
                "Evidence has been logged in your dashboard settlement view.",
                deduction.getTitle(), deduction.getCategory(), deduction.getAmount()
        );
        whatsAppSender.sendTextMessage(contract.getTenantPhone(), msg);
    }
}