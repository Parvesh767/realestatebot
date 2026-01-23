package com.risingbee.realestate.automation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.auth.dto.BrokerResponse;
import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.repo.BrokerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BrokerService {

    private final BrokerRepository brokerRepository;

    public Broker createForOnboarding(String phone) {

        Broker broker = brokerRepository
            .findByPhone(phone)
            .orElseGet(() -> {
                Broker b = new Broker(phone);
                b.startOnboarding();   // 👈 domain method
                return brokerRepository.save(b);
            });

        // 🛡️ Heal legacy / corrupted brokers
        if (broker.getOnboardingStep() == null) {
            log.warn(
                "Broker {} found without onboardingStep. Resetting to START.",
                broker.getId()
            );
            broker.startOnboarding();
            brokerRepository.save(broker);
        }

        return broker;
    }

    public BrokerResponse getBrokerProfile(Long brokerId) {

        Broker broker = brokerRepository.findById(brokerId)
            .orElseThrow(() ->
                new IllegalStateException("Broker not found")
            );

        return BrokerResponse.from(broker);
    }
}
