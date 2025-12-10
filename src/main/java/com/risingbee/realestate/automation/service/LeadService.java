package com.risingbee.realestate.automation.service;

import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.repo.LeadRepository;
import com.risingbee.realestate.automation.repo.BrokerRepository;
import com.risingbee.realestate.automation.tenant.BrokerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LeadService {

    private final LeadRepository leadRepository;
    private final BrokerRepository brokerRepository;

    public Lead createFromParsed(String phone, String rawMessage, String bhk, Integer minBudget, Integer maxBudget, String location) {
        Broker broker = BrokerContext.get();
        if (broker == null) throw new IllegalStateException("No broker in context");

        Lead lead = Lead.builder()
                .phoneNumber(phone)
                .rawMessage(rawMessage)
                .bhk(bhk)
                .minBudget(minBudget)
                .maxBudget(maxBudget)
                .location(location)
                .createdAt(Instant.now())
                .broker(broker)
                .build();

        leadRepository.save(lead);
        return lead;
    }

    public List<Lead> findAllForCurrentBroker() {
        Long brokerId = BrokerContext.id();
        if (brokerId == null) throw new IllegalStateException("No broker in context");
        return leadRepository.findByBrokerIdOrderByCreatedAtDesc(brokerId);
    }
}
