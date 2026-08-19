package com.risingbee.realestate.profile.service;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.auth.dto.AuthResponse;
import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.profile.dto.BasicInfoRequest;
import com.risingbee.realestate.profile.dto.CompleteProfileRequest;
import com.risingbee.realestate.profile.dto.ProfileStage;
import com.risingbee.realestate.profile.dto.ProfileStatusResponse;
import com.risingbee.realestate.profile.dto.SetRoleRequest;
import com.risingbee.realestate.profile.utility.ProfileStageResolver;
import com.risingbee.realestate.security.JwtUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {
	
	 private final JwtUtil jwtUtil;

    private final AccountRepository accountRepository;
    
    
    
    
    public ProfileStatusResponse getStatus(Account account) {

        ProfileStage stage =
            ProfileStageResolver.resolve(account);

        return buildResponse(account, stage);
    }
    
    public ProfileStatusResponse getStatusByExternalId(String externalId) {

        Account account = accountRepository
            .findByExternalId(externalId).orElse(null);
          
        
        ProfileStage stage = ProfileStageResolver.resolve(account);

        return buildResponse(account,stage);
    }
    
    
    

    // -------- STATUS --------
    public ProfileStatusResponse getStatus() {

        Account account = getCurrentAccount();

        ProfileStage stage = ProfileStageResolver.resolve(account);

        return buildResponse(account, stage);
    }

    
    

    public AuthResponse setRoleAndRefreshToken(SetRoleRequest request) {

        if (request.role() == null) {
            throw new IllegalArgumentException("Role is required");
        }

        Account account = getAccount();

        if (account.getType() != null) {
            throw new IllegalStateException("Role already set");
        }

        // 1) update role
        account.setType(request.role());

        // 2) resolve new stage
        ProfileStage stage = ProfileStageResolver.resolve(account);

        // 3) build new actor
        Actor actor = new Actor(
                account.getType(),
                account.getExternalId(),
                account.getId()
        );

        // 4) generate fresh token with updated role
        String token = jwtUtil.generateToken(actor);

        // 5) return enriched response
        return new AuthResponse(
                token,
                account.getId(),
                account.getExternalId(),
                account.getType(),
                stage.name(),
                ProfileStageResolver.nextStep(stage)
        );
    }

    private Account getAccount() {
        Actor actor = ActorContext.get();

        if (actor == null || actor.internalId() == null) {
            throw new IllegalStateException("Unauthenticated");
        }

        return accountRepository.findById(actor.internalId())
                .orElseThrow(() -> new IllegalStateException("Account not found"));
    }
    // -------- ROLE --------
    public void setRole(SetRoleRequest request) {

        Account account = getCurrentAccount();

        if (account.getType() != null) {
            throw new IllegalStateException("Role already set");
        }

        account.setType(request.role());
    }

    // -------- BASIC INFO --------
    public void updateBasicInfo(BasicInfoRequest request) {

        Account account = getCurrentAccount();

        account.setName(request.name());
        account.setEmail(request.email());
    }

    // -------- COMPLETE --------
    public ProfileStatusResponse completeProfile(CompleteProfileRequest request) {

        Account account = getCurrentAccount();

        // set everything together
        account.setType(request.role());
        account.setName(request.name());
        account.setEmail(request.email());

        account.setProfileCompleted(true);

        ProfileStage stage = ProfileStageResolver.resolve(account);

        return buildResponse(account, stage);
    }

    // -------- COMMON --------
    private Account getCurrentAccount() {
        Actor actor = ActorContext.get();

        if (actor == null || actor.internalId() == null) {
            throw new IllegalStateException("Unauthenticated");
        }

        return accountRepository.findById(actor.internalId())
                .orElseThrow(() -> new IllegalStateException("Account not found"));
    }

    private ProfileStatusResponse buildResponse(Account account, ProfileStage stage) {
        return new ProfileStatusResponse(
            account.getId(),
            account.getExternalId(),
            account.getType(),
            stage.name(),
            ProfileStageResolver.nextStep(stage),
            account.isProfileCompleted()
        );
    }
    
    
    
    
    
}