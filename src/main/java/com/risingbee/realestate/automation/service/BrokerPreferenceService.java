package com.risingbee.realestate.automation.service;


import java.util.List;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.auth.AuthorizationService;
import com.risingbee.realestate.automation.actor.enums.Capability;
import com.risingbee.realestate.automation.domain.BrokerPreference;
import com.risingbee.realestate.automation.repo.BrokerPreferenceRepository;
import com.risingbee.realestate.automation.actor.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrokerPreferenceService {

    private final BrokerPreferenceRepository repo;
    private final BrokerAreaService brokerAreaService;
    private final AuthorizationService authz;


    /* ======================
       Matching / Acceptance
       ====================== */

    public boolean accepts(
    	    Long brokerAccountId,
    	    String cityCode,
    	    String localityCode,
    	    Integer requestedMaxBudget
    	) {
    	    BrokerPreference pref =
    	        repo.findById(brokerAccountId).orElse(null);

    	    if (pref == null) return true;

    	    if (requestedMaxBudget != null &&
    	        pref.getMaxBudget() != null &&
    	        requestedMaxBudget > pref.getMaxBudget()) {
    	        return false;
    	    }

    	    if (cityCode == null) return true;

    	    return brokerAreaService.accepts(
    	        brokerAccountId,
    	        cityCode,
    	        localityCode
    	    );
    	}


    /* ======================
       Mutations (Onboarding)
       ====================== */

    @Transactional
    public void updateBudget(
        Actor actor,
        Integer min,
        Integer max
    ) {
        authz.require(actor, Capability.BROKER_ONBOARDING);

        Long accountId = actor.internalId();
        if (accountId == null) {
            throw new IllegalStateException("Actor has no broker identity");
        }

        BrokerPreference pref =
            repo.findById(accountId)
                .orElseGet(() -> new BrokerPreference(accountId));

        pref.updateBudget(min, max);
        repo.save(pref);
    }
    
    @Transactional
    public void updateBhks(
        Actor actor,
        List<String> bhks
    ) {
        authz.require(actor, Capability.BROKER_ONBOARDING);

        Long accountId = actor.internalId();
        if (accountId == null) {
            throw new IllegalStateException("Actor has no broker identity");
        }

        BrokerPreference pref =
            repo.findById(accountId)
                .orElseGet(() -> new BrokerPreference(accountId));

        pref.updateBhks(bhks);
        repo.save(pref);
    }


}
