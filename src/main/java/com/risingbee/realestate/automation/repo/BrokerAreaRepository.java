package com.risingbee.realestate.automation.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.risingbee.realestate.automation.domain.BrokerArea;

public interface BrokerAreaRepository extends JpaRepository<BrokerArea, Long> {

	List<BrokerArea> findByBrokerId(Long brokerId);

	boolean existsByBrokerIdAndCityCodeAndLocalityCode(Long brokerId, String cityCode, String localityCode);

	boolean existsByBrokerIdAndCityCodeAndLocalityCodeIsNull(Long brokerId, String cityCode);
}
