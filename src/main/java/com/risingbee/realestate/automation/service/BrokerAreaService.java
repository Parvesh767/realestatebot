package com.risingbee.realestate.automation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.domain.BrokerArea;
import com.risingbee.realestate.automation.repo.BrokerAreaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrokerAreaService {

    private final BrokerAreaRepository repo;

    public void save(Long brokerId, List<String> areas) {
        repo.deleteAll(repo.findByBrokerId(brokerId));

        areas.forEach(a -> {
            BrokerArea ba = new BrokerArea();
            ba.setBrokerId(brokerId);
            ba.setArea(a.toLowerCase());
            repo.save(ba);
        });
    }

    public List<String> getAreas(Long brokerId) {
        return repo.findByBrokerId(brokerId)
                   .stream()
                   .map(BrokerArea::getArea)
                   .toList();
    }
}
