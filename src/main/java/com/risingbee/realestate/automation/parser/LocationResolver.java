package com.risingbee.realestate.automation.parser;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.location.domain.Locality;
import com.risingbee.realestate.location.domain.LocationAlias;
import com.risingbee.realestate.location.repo.CityRepository;
import com.risingbee.realestate.location.repo.LocalityRepository;
import com.risingbee.realestate.location.repo.LocationAliasRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationResolver {

    private final LocationAliasRepository aliasRepo;
    private final LocalityRepository localityRepo;
    private final CityRepository cityRepo;

    // Existing method (searcher flow)
    public Optional<ResolvedLocation> resolve(ParsedRequest parsed) {
        return resolveText(parsed.location());
    }

    public Optional<ResolvedLocation> resolveText(List<String> tokens) {

        if (tokens == null || tokens.isEmpty()) {
            return Optional.empty();
        }

        // Join tokens into searchable text
        String normalized = String.join(" ", tokens).toLowerCase();

        // 1️⃣ Alias → locality
        Optional<LocationAlias> alias =
            aliasRepo.findBestMatch(normalized);

        if (alias.isPresent()) {
            Locality l = alias.get().getLocality();
            return Optional.of(
                new ResolvedLocation(
                    l.getCity().getCode(),
                    l.getCode()
                )
            );
        }

        // 2️⃣ Direct locality
        Optional<Locality> locality =
            localityRepo.findBestMatch(normalized);

        if (locality.isPresent()) {
            Locality l = locality.get();
            return Optional.of(
                new ResolvedLocation(
                    l.getCity().getCode(),
                    l.getCode()
                )
            );
        }

        // 3️⃣ City only
        return cityRepo.findBestMatch(normalized)
            .map(c -> new ResolvedLocation(c.getCode(), null));
    }

}
