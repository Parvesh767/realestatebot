package com.risingbee.realestate.automation.domain;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.risingbee.realestate.converter.StringListJsonConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "broker_preferences")
@Getter
public class BrokerPreference {

	@Id
	private Long brokerId;

	private Integer minBudget;
	private Integer maxBudget;

	/** Comma-safe, normalized list */
	@Column(columnDefinition = "json")
	@JdbcTypeCode(SqlTypes.JSON)
	@Convert(converter = StringListJsonConverter.class)
	private List<String> bhks = new ArrayList<>();

	/*
	 * ----------------- JPA requirement -----------------
	 */
	protected BrokerPreference() {
	}

	/*
	 * ----------------- Domain construction -----------------
	 */
	public BrokerPreference(Long brokerId, Integer minBudget, Integer maxBudget) {
		this.brokerId = brokerId;
		this.minBudget = minBudget;
		this.maxBudget = maxBudget;
	}

	/*
	 * ----------------- Domain mutation -----------------
	 */
	public void updateBudget(Integer minBudget, Integer maxBudget) {
		this.minBudget = minBudget;
		this.maxBudget = maxBudget;
	}

	/*
	 * ----------------- Domain logic (optional) -----------------
	 */
	public boolean allows(Integer requestedMaxBudget) {
		if (requestedMaxBudget == null || maxBudget == null) {
			return true;
		}
		return requestedMaxBudget <= maxBudget;
	}

	public BrokerPreference(Long brokerId) {
		this.brokerId = brokerId;
	}

	public void updateBhks(List<String> bhks) {
		this.bhks.clear();
		this.bhks.addAll(bhks.stream().map(String::trim).map(String::toUpperCase).toList());
	}

	public boolean allowsBhk(String bhk) {
		return bhks.isEmpty() || bhks.contains(bhk);
	}
}
