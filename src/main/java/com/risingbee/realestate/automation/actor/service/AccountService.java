package com.risingbee.realestate.automation.actor.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.enums.ActorType;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.exception.ResourceNotFoundException;
import com.risingbee.realestate.profile.dto.ProfileStatusResponse;
import com.risingbee.realestate.profile.service.ProfileService;
import com.risingbee.realestate.profile.service.ProfileServiceWhatsApp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final ProfileService profileService;
    private final ProfileServiceWhatsApp profileServiceWhatsApp;

    /**
     * 🔥 FIX: Look up Account by primary key ID
     */
    @Transactional(readOnly = true)
    public Optional<Account> findById(Long id) {
        if (id == null) return Optional.empty();
        return accountRepository.findById(id);
    }

    /**
     * Create or fetch an Account for an Actor that has no internal identity.
     */
    public Account createFromActor(Actor actor, String message) {

        if (actor.internalId() != null) {
            throw new IllegalStateException(
                "Actor already has internalId: " + actor
            );
        }
        
        log.debug("getting status for actor: {}", actor.externalId());
        
        ProfileStatusResponse status = profileService.getStatusByExternalId(actor.externalId());
        
        log.debug("status {}", status);
        
        if (!status.profileCompleted()) {
            profileServiceWhatsApp.handleProfileFlow(message, status);
        }

        String externalId = actor.externalId();

        return accountRepository
            .findByExternalId(externalId)
            .orElseGet(() -> {

                ActorType type = actor.role() != null ? actor.role() : null;

                Account account = new Account(type, externalId);
                Account saved = accountRepository.save(account);

                log.info(
                    "Created new Account id={} for externalId={}, type={}",
                    saved.getId(), externalId, type
                );

                return saved;
            });
    }
    
    public Account upgradeToBroker(Actor actor) {

        Long accountId = actor.internalId();

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found for id: " + accountId));

        if (account.getType() == ActorType.BROKER) {
            return account;
        }

        account.setType(ActorType.BROKER);

        log.info("Account {} upgraded to BROKER", accountId);

        return account;
    }
    
    @Transactional
    public Account getOrCreate(String externalId) {

        return accountRepository
            .findByExternalId(externalId)
            .orElseGet(() -> {

                Account account = new Account();

                account.setExternalId(externalId);
                account.setPhone(externalId);

                account.setActive(true);
                account.setVerified(false);
                account.setProfileCompleted(false);

                return accountRepository.save(account);
            });
    }
    
    /**
     * 🔥 Add lead unlock credits to an Account (e.g. after payment webhook confirmation)
     *
     * @param accountId Primary key ID of the Account
     * @param credits   Number of credits to add (e.g., 30 or 100)
     * @return Updated Account entity
     */
    @Transactional
    public Account addCreditsToAccount(Long accountId, int credits) {
        if (accountId == null) {
            throw new IllegalArgumentException("AccountId cannot be null");
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found for id: " + accountId));

        account.addCredits(credits);
        Account updated = accountRepository.save(account);

        log.info("Added {} credits to Account id={}. New credits balance={}", 
                credits, accountId, updated.getCreditsBalance());

        return updated;
    }
}