package com.risingbee.realestate.automation.repo;

import com.risingbee.realestate.automation.domain.PropertyUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PropertyUnitRepository extends JpaRepository<PropertyUnit, Long> {
    List<PropertyUnit> findByPropertyId(Long propertyId);
}