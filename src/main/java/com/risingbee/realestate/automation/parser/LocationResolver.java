package com.risingbee.realestate.automation.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.location.domain.Locality;
import com.risingbee.realestate.location.domain.LocationAlias;
import com.risingbee.realestate.location.repo.CityRepository;
import com.risingbee.realestate.location.repo.LocalityRepository;
import com.risingbee.realestate.location.repo.LocationAliasRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationResolver {

	private final LocationAliasRepository aliasRepo;
	private final LocalityRepository localityRepo;
	private final CityRepository cityRepo;

	// Existing method (searcher flow)
	public Optional<ResolvedLocation> resolve(ParsedRequest parsed) {
		return resolveText(parsed.tokens());
	}

	public Optional<ResolvedLocation> resolveText(List<String> tokens) {

		if (tokens == null || tokens.isEmpty()) {
			return Optional.empty();
		}

//        String normalized = String.join(" ", tokens).toLowerCase();
//        String normalized = String.join(" ", tokens)
//        	    .toLowerCase()
//        	    .replaceAll("[^a-z0-9 ]", "")
//        	    .trim();
//        

		// 1️⃣ Alias
		List<LocationAlias> aliases = aliasRepo.findAll();
		
		log.info("all aliases list size : {}" , aliases.size());
		

//        List<LocationAlias> aliases = aliasRepo.findBestMatches(normalized);

		Map<LocationAlias,Integer> scored = new HashMap<>();
		for (LocationAlias alias : aliases) {

			List<String> aliasTokens = Arrays.stream(alias.getAlias().split(" ")).map(String::toLowerCase).toList();

			int score = 0;

			for (String token : tokens) {
				if (aliasTokens.contains(token)) {
					score++;
				}
			}

			if (score > 0) {
				scored.put(alias, score);
			}
		}
		
		
		if (scored.isEmpty()) {
		    log.warn("No alias match");
		    return Optional.empty();
		}
		
		
		List<LocationAlias> topAliases = scored.entrySet().stream()
			    .sorted((a, b) -> b.getValue() - a.getValue())
			    .limit(3) // 🔥 top 3 matches
			    .map(Map.Entry::getKey)
			    .toList();
		
		
		List<ResolvedLocation> locations = topAliases.stream()
			    .map(alias -> {
			        Locality l = alias.getLocality();
			        return new ResolvedLocation(
			            l.getCity().getCode(),
			            l.getCode()
			        );
			    })
			    .limit(5)
			    .distinct()
			    .toList();

//		List<ResolvedLocation> topLocations = scored.entrySet().stream()
//		    .sorted((a, b) -> b.getValue() - a.getValue())
//		    .limit(3)
//		    .map(entry -> {
//		        Locality l = entry.getKey().getLocality();
//		        return new ResolvedLocation(
//		            l.getCity().getCode(),
//		            l.getCode()
//		        );
//		    })
//		    .toList();
		log.info("top locations : {}",locations);

		// return best OR list depending on your design
		return Optional.of(locations.get(0)); 

//		LocationAlias best = scored.entrySet().stream()
//			    .sorted((a, b) -> b.getValue() - a.getValue())
//			    .map(Map.Entry::getKey)
//			    .findFirst()
//			    .orElse(null);
//		
//		
//		if (best != null) {
//		    Locality l = best.getLocality();
//		    return Optional.of(new ResolvedLocation(
//		        l.getCity().getCode(),
//		        l.getCode()
//		    ));
//		}
//		
//		if (!aliases.isEmpty()) {
//			Locality l = pickBestLocality(aliases.stream().map(LocationAlias::getLocality).toList());
//			return Optional.of(new ResolvedLocation(l.getCity().getCode(), l.getCode()));
//		}
//		
//		log.warn("Input and alias not match");
//		return Optional.of(new ResolvedLocation(null, null));
//		
//		
//		
//		
//		LocationAlias best = scored.entrySet().stream()
//			    .sorted((a, b) -> b.getValue() - a.getValue())
//			    .map(Map.Entry::getKey)
//			    .findFirst()
//			    .orElse(null);
//
//		// 2️⃣ Locality
////		List<Locality> localities = localityRepo.findBestMatches(normalized);
//
//		if (!localities.isEmpty()) {
//			Locality l = pickBestLocality(localities);
//			return Optional.of(new ResolvedLocation(l.getCity().getCode(), l.getCode()));
//		}
//
//		log.info("Input = {}", normalized);
//		log.info("Alias matches = {}", aliases.size());
//		log.info("Locality matches = {}", localities.size());
//
//		// 3️⃣ City fallback
//		return cityRepo.findBestMatch(normalized).map(c -> new ResolvedLocation(c.getCode(), null));
	}
	
	
	

	private Locality pickBestLocality(List<Locality> list) {

		if (list.size() == 1)
			return list.get(0);

		// 🔥 MVP strategy
		return list.stream().filter(l -> l.getCity().getCode().equalsIgnoreCase("GURGAON")).findFirst()
				.orElse(list.get(0));
	}

}
