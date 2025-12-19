package com.risingbee.realestate.automation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.domain.BrokerPreference;
import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.repo.BrokerPreferenceRepository;
import com.risingbee.realestate.automation.tenant.BrokerContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrokerPreferenceService {

    private final BrokerPreferenceRepository repo;
    private final BrokerAreaService brokerAreaService;

    public boolean accepts(ParsedRequest req) {

        BrokerPreference pref =
            repo.findById(BrokerContext.id()).orElse(null);

        if (pref == null) return true; // no restrictions yet

        // budget filter
        if (req.maxBudget() != null &&
            pref.getMaxBudget() != null &&
            req.maxBudget() > pref.getMaxBudget()) {
            return false;
        }

        // area filter
        if (req.location() != null) {
            List<String> areas =
                brokerAreaService.getAreas(BrokerContext.id());

            boolean match =
                areas.stream().anyMatch(req.location()::contains);

            if (!match) return false;
        }

        return true;
    }

//	public void saveBhks(Long id, List<String> bhks) {
//		
//		repo.saveAll(id,bhks);
//		
//	}

}
