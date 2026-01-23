package com.risingbee.realestate.automation.domain;

import com.risingbee.realestate.automation.parser.ResolvedLocation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "broker_areas", uniqueConstraints = @UniqueConstraint(columnNames = { "broker_id", "city_code",
		"locality_code" }))

@Getter
@Setter
public class BrokerArea {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "broker_id", nullable = false)
	private Long brokerId;

	@Column(name = "city_code", nullable = false)
	private String cityCode;

	@Column(name = "locality_code")
	private String localityCode;

	protected BrokerArea() {
	}

	private BrokerArea(Long brokerId, String cityCode, String localityCode) {
		this.brokerId = brokerId;
		this.cityCode = cityCode;
		this.localityCode = localityCode;
	}

	public static BrokerArea from(Long brokerId, ResolvedLocation location) {
		return new BrokerArea(brokerId, location.city(), // maps to city_code
				location.locality() // maps to locality_code (nullable)
		);
	}
}
