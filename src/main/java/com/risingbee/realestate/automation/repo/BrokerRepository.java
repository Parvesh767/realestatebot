package com.risingbee.realestate.automation.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.risingbee.realestate.automation.domain.Broker;

public interface BrokerRepository extends JpaRepository<Broker, Long> {
    Optional<Broker> findByApiKey(String apiKey);

    Optional<Broker> findByPhone(String phone);
}