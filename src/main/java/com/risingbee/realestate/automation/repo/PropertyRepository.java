package com.risingbee.realestate.automation.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.risingbee.realestate.automation.domain.Property;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    /**
     * Public search query used by SearcherSearchHandler via PropertyService.
     * Filter active properties by BHK, Location, and Price range.
     */
    @Query("""
        select p from Property p
        where p.active = true
          and (:bhk is null or p.bhk = :bhk)
          and (:cityCode is null or p.cityCode = :cityCode)
          and (:localityCode is null or p.localityCode = :localityCode)
          and (:min is null or p.price >= :min)
          and (:max is null or p.price <= :max)
        order by p.price asc, p.id desc
    """)
    List<Property> searchPublic(
        @Param("bhk") String bhk,
        @Param("cityCode") String cityCode,
        @Param("localityCode") String localityCode,
        @Param("min") Long min,  // 🔑 Updated from Integer to Long for INR Crore/Lakh handling
        @Param("max") Long max   // 🔑 Updated from Integer to Long for INR Crore/Lakh handling
    );

    /**
     * Retrieves all properties owned by a specific account ID.
     */
    List<Property> findByOwnerAccountId(Long ownerAccountId);
}