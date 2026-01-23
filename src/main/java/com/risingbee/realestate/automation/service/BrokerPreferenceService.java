package com.risingbee.realestate.automation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.enums.Capability;
import com.risingbee.realestate.automation.domain.BrokerPreference;
import com.risingbee.realestate.automation.repo.BrokerPreferenceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrokerPreferenceService {

    private final BrokerPreferenceRepository repo;
    private final BrokerAreaService brokerAreaService;

    /* ======================
       Matching / Acceptance
       ====================== */

    public boolean accepts(
            String cityCode,
            String localityCode,
            Integer requestedMaxBudget
    ) {

        // 🔐 Authorization boundary
        if (!ActorContext.hasCapability(Capability.ADD_PROPERTY)) {
            return true; // actors without preferences never block
        }

        Long accountId = ActorContext.get().internalId();
        if (accountId == null) {
            return true;
        }

        BrokerPreference pref =
                repo.findById(accountId).orElse(null);

        // no preferences → accept all
        if (pref == null) return true;

        // -------------------
        // Budget filter
        // -------------------
        if (requestedMaxBudget != null &&
            pref.getMaxBudget() != null &&
            requestedMaxBudget > pref.getMaxBudget()) {
            return false;
        }

        // -------------------
        // Location filter
        // -------------------
        if (cityCode == null) {
            // unresolved location → do not block
            return true;
        }

        return brokerAreaService.accepts(
                accountId,
                cityCode,
                localityCode
        );
    }

    /* ======================
       Mutations (Onboarding)
       ====================== */

    @Transactional
    public void updateBudget(
            Long accountId,
            Integer min,
            Integer max
    ) {

        BrokerPreference pref =
                repo.findById(accountId)
                        .orElseGet(() -> new BrokerPreference(accountId));

        pref.updateBudget(min, max);
        repo.save(pref);
    }

    @Transactional
    public void updateBhks(
            Long accountId,
            List<String> bhks
    ) {

        BrokerPreference pref =
                repo.findById(accountId)
                        .orElseGet(() -> new BrokerPreference(accountId));

        pref.updateBhks(bhks);
        repo.save(pref);
    }
}
