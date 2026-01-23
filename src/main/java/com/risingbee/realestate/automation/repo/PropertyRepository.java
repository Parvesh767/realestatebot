package com.risingbee.realestate.automation.repo;

import com.risingbee.realestate.automation.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

//    List<Property> findByBrokerIdAndActiveTrue(Long brokerId);

//    @Query("""
//    	    select p from Property p
//    	    where p.broker.id = :brokerId
//    	      and p.active = true
//    	      and (:bhk is null or p.bhk = :bhk)
//    	      and (:cityCode is null or p.cityCode = :cityCode)
//    	      and (:localityCode is null or p.localityCode = :localityCode)
//    	      and (:minBudget is null or p.price >= :minBudget)
//    	      and (:maxBudget is null or p.price <= :maxBudget)
//    	    order by p.price asc
//    	""")
//    	List<Property> findMatchesForBroker(
//    	    @Param("brokerId") Long brokerId,
//    	    @Param("bhk") String bhk,
//    	    @Param("cityCode") String cityCode,
//    	    @Param("localityCode") String localityCode,
//    	    @Param("minBudget") Integer minBudget,
//    	    @Param("maxBudget") Integer maxBudget
//    	);

    
    
    @Query("""
    	    select p from Property p
    	    where p.active = true
    	      and (:bhk is null or p.bhk = :bhk)
    	      and (:cityCode is null or p.cityCode = :cityCode)
    	      and (:localityCode is null or p.localityCode = :localityCode)
    	      and (:min is null or p.price >= :min)
    	      and (:max is null or p.price <= :max)
    	    order by p.price asc
    	""")
    	List<Property> searchPublic(
    	    @Param("bhk") String bhk,
    	    @Param("cityCode") String cityCode,
    	    @Param("localityCode") String localityCode,
    	    @Param("min") Integer min,
    	    @Param("max") Integer max
    	);

    List<Property> findByOwnerAccountIdAndActiveTrue(Long ownerAccountId);





}

