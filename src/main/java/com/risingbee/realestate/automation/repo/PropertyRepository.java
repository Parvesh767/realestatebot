package com.risingbee.realestate.automation.repo;

import com.risingbee.realestate.automation.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByBrokerIdAndActiveTrue(Long brokerId);

    // Simple match query: filters are nullable; use OR conditions to skip filtering when parameter is null.
    @Query("""
        SELECT p FROM Property p
        WHERE p.broker.id = :brokerId
          AND p.active = true
          AND (:bhk IS NULL OR p.bhk = :bhk)
          AND (:location IS NULL OR LOWER(p.area) LIKE LOWER(CONCAT('%', :location, '%')))
          AND (:minBudget IS NULL OR p.price >= :minBudget)
          AND (:maxBudget IS NULL OR p.price <= :maxBudget)
        ORDER BY p.price ASC
        """)
    List<Property> findMatches(
        @Param("brokerId") Long brokerId,
        @Param("bhk") String bhk,
        @Param("location") String location,
        @Param("minBudget") Integer minBudget,
        @Param("maxBudget") Integer maxBudget
    );
}
