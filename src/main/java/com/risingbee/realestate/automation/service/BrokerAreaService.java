package com.risingbee.realestate.automation.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

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

   
    
    public boolean save(Long brokerId, List<String> rawAreas) {

        repo.deleteAll(repo.findByBrokerId(brokerId));

        boolean savedAtLeastOne = false;

        for (String raw : rawAreas) {

            // tokenize the raw phrase
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

    	    // city-wide broker
    	    return repo.existsByBrokerIdAndCityCodeAndLocalityCodeIsNull(
    	        brokerId, cityCode
    	    );
    	}
    
    
}
