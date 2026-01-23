package com.risingbee.realestate.automation.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.enums.Capability;
import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.repo.LeadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LeadService {

	private final LeadRepository leadRepository;

	/**
	 * Create a lead from a matched property. Works for OWNER or BROKER properties.
	 */
	public Lead createFromMatchedProperty(String phone, Property property, String rawMessage, String cityCode,
			String localityCode) {

		Long ownerAccountId = property.getOwnerAccountId();

		Lead lead = new Lead(phone, ownerAccountId, property.getBhk(), property.getPrice(), property.getPrice(),
				cityCode, localityCode, rawMessage);

		return leadRepository.save(lead);
	}

	/**
	 * Find all leads for the current actor (OWNER or BROKER).
	 */
	public List<Lead> findAllForCurrentActor() {

		ActorContext.requireCapability(Capability.ADD_PROPERTY);

		Long accountId = ActorContext.get().internalId();
		if (accountId == null) {
			throw new IllegalStateException("Actor has no accountId");
		}

		return leadRepository.findByOwnerAccountIdOrderByCreatedAtDesc(accountId);
	}

	public Optional<Lead> findLatestByPhone(String phone) {
		return leadRepository.findTopByPhoneNumberOrderByCreatedAtDesc(phone);
	}
}
