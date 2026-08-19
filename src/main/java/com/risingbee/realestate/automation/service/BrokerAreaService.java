package com.risingbee.realestate.automation.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.auth.AuthorizationService;
import com.risingbee.realestate.automation.actor.enums.Capability;
import com.risingbee.realestate.automation.actor.*;
import com.risingbee.realestate.automation.domain.BrokerArea;
import com.risingbee.realestate.automation.parser.LocationResolver;
import com.risingbee.realestate.automation.parser.ResolvedLocation;
import com.risingbee.realestate.automation.repo.BrokerAreaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrokerAreaService {

    private final BrokerAreaRepository repo;
    private final LocationResolver locationResolver;
    private final AuthorizationService authz;

    public boolean save(Actor actor, List<String> rawAreas) {

        authz.require(actor, Capability.BROKER_ONBOARDING);

        Long brokerId = actor.internalId();
        if (brokerId == null) {
            throw new IllegalStateException(
                "Actor has no broker identity"
            );
        }

        repo.deleteAll(repo.findByBrokerId(brokerId));

        boolean savedAtLeastOne = false;

        for (String raw : rawAreas) {

            List<String> tokens =
                Arrays.stream(raw.split("\\s+"))
                      .map(String::toLowerCase)
                      .toList();

            Optional<ResolvedLocation> resolved =
                locationResolver.resolveText(tokens);

            if (resolved.isPresent()) {
                repo.save(
                    BrokerArea.from(brokerId, resolved.get())
                );
                savedAtLeastOne = true;
            }
        }

        return savedAtLeastOne;
    }

    /* ---------- READ-ONLY METHODS ---------- */

    public List<String> getAreas(Long brokerId) {
        return repo.findByBrokerId(brokerId)
                   .stream()
                   .map(BrokerArea::getLocalityCode)
                   .toList();
    }

    public boolean accepts(
        Long brokerId,
        String cityCode,
        String localityCode
    ) {
        if (localityCode != null &&
            repo.existsByBrokerIdAndCityCodeAndLocalityCode(
                brokerId, cityCode, localityCode
            )) {
            return true;
        }

        return repo.existsByBrokerIdAndCityCodeAndLocalityCodeIsNull(
            brokerId, cityCode
        );
    }
}

