package com.risingbee.realestate.automation.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.auth.AuthorizationService;
import com.risingbee.realestate.auth.dto.BrokerResponse;
import com.risingbee.realestate.automation.actor.*;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.enums.*;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.actor.service.AccountService;
import com.risingbee.realestate.automation.repo.BrokerRepository;
import com.risingbee.realestate.automation.domain.*;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BrokerService {
	
	private final AuthorizationService authz;
	private final BrokerRepository brokerRepository;
	private final AccountService accountService;
	private final AccountRepository accountRepository;


	public Broker activateBroker(Actor actor) {

	    authz.require(actor, Capability.BROKER_ONBOARDING);

//	    Optional<Account> accountId = accountRepository.findById(actor.internalId());
//	    
//	    if (accountId.of == null) {
//	        throw new IllegalStateException("Actor has no account identity");
//	    }

	    
	    Account account = accountRepository
	    	    .findById(actor.internalId())
	    	    .orElseThrow();

	    	account.setType(ActorType.BROKER);
	    
	    Broker broker = brokerRepository
	        .findById(account.getId())
	        .orElseGet(() -> brokerRepository.save(
	            Broker.createFromActor(actor)
	        ));

	    broker.activate(); 
	    
	    // 🔥 ADD THIS (MOST IMPORTANT)
//	    Optional<Account> account = accountRepository.findByExternalId(actor.externalId()); // or fetch via repo
	    
	    accountService.upgradeToBroker(actor);	
	    return broker;
	}
	
	
	public BrokerResponse getMyProfile(Actor actor) {

	    authz.require(actor, Capability.BROKER_ONBOARDING);

	    Long brokerId = actor.internalId();
	    if (brokerId == null) {
	        throw new IllegalStateException("Actor is not a broker");
	    }

	    Broker broker = brokerRepository.findById(brokerId)
	        .orElseThrow(() ->
	            new IllegalStateException("Broker not found")
	        );

	    return BrokerResponse.from(broker);
	}
}
