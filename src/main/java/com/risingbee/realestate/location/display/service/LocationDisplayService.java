package com.risingbee.realestate.location.display.service;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.location.domain.City;
import com.risingbee.realestate.location.domain.Locality;
import com.risingbee.realestate.location.repo.CityRepository;
import com.risingbee.realestate.location.repo.LocalityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationDisplayService {

    private final LocalityRepository localityRepository;
    private final CityRepository cityRepository;

    public String localityName(String localityCode) {
        if (localityCode == null) return null;

        return localityRepository
            .findByCode(localityCode)
            .map(Locality::getName)
            .orElse(localityCode); // fallback
    }

    public String cityName(String cityCode) {
        if (cityCode == null) return null;

        return cityRepository
            .findByCode(cityCode)
            .map(City::getName)
            .orElse(cityCode);
    }
}

