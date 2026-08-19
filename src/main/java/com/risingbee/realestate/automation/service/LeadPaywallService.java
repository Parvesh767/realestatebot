package com.risingbee.realestate.automation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.service.AccountService;
import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.dto.LeadResponseDTO;
import com.risingbee.realestate.automation.exception.AccessDeniedException;
import com.risingbee.realestate.automation.exception.InsufficientCreditsException;
import com.risingbee.realestate.automation.exception.ResourceNotFoundException;
import com.risingbee.realestate.automation.repo.LeadRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LeadPaywallService {

    private final LeadRepository leadRepository;
    private final AccountService accountService;

    public LeadResponseDTO unlockLeadForBroker(Actor actor, Long leadId) {
        Long accountId = actor.internalId();
        if (accountId == null) {
            throw new IllegalStateException("Actor has no associated account identity");
        }

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));

        if (!lead.getOwnerAccountId().equals(accountId)) {
            throw new AccessDeniedException("You do not have access to this lead");
        }

        if (Boolean.TRUE.equals(lead.getUnlocked())) {
            return toDTO(lead);
        }

        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.deductCredit(1)) {
            throw new InsufficientCreditsException("Insufficient credits to unlock lead. Please recharge!");
        }

        lead.unlock();
        leadRepository.save(lead);

        return toDTO(lead);
    }

    public LeadResponseDTO toDTO(Lead lead) {
        boolean showFull = Boolean.TRUE.equals(lead.getUnlocked());
        
        String phoneToDisplay = showFull 
                ? lead.getPhoneNumber() 
                : maskPhoneNumber(lead.getPhoneNumber());

        return new LeadResponseDTO(
            lead.getId(),
            phoneToDisplay,
            lead.getBhk(),
            lead.getMinBudget(),
            lead.getMaxBudget(),
            lead.getCityCode(),
            lead.getLocalityCode(),
            lead.getUnlocked(),
            lead.getCreatedAt()
        );
    }

    private String maskPhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.length() < 8) return "XXXXXXXXXX";
        int len = rawPhone.length();
        return rawPhone.substring(0, len - 6) + "XXXXXX";
    }
}