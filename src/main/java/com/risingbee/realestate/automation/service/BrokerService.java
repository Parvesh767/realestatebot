package com.risingbee.realestate.automation.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.repo.BrokerRepository;
import com.risingbee.realestate.enums.BrokerStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BrokerService {
	
	 private final BrokerRepository brokerRepository;

	public Broker findOrCreateByPhone(String phone) {
	    return brokerRepository.findByPhone(phone)
	        .orElseGet(() -> {
	            Broker b = new Broker();
	            b.setPhone(phone);
//	            b.setStatus(BrokerStatus.CREATED);
//	            b.setLastInteraction(Instant.now());
	            return brokerRepository.save(b);
	        });
	}
	
}
