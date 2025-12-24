package com.risingbee.realestate.automation.repo;

import com.risingbee.realestate.automation.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByBrokerIdAndActiveTrue(Long brokerId);

    @Query(
        value = """
        SELECT *
        FROM properties p
        WHERE p.broker_id = :brokerId
          AND p.active = true         
          AND (:bhk IS NULL OR p.bhk = :bhk)
          AND (:area IS NULL OR p.area ILIKE '%' || :area || '%')
          AND (:minBudget IS NULL OR p.price >= :minBudget)
          AND (:maxBudget IS NULL OR p.price <= :maxBudget)
        ORDER BY p.price ASC
        """,
        nativeQuery = true
    )
    List<Property> findMatches(
        @Param("brokerId") Long brokerId,
        @Param("title") String title,
        @Param("bhk") String bhk,
        @Param("area") String area,
        @Param("minBudget") Integer minBudget,
        @Param("maxBudget") Integer maxBudget
    );
}

