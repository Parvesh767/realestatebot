package com.risingbee.realestate.automation.tenancy.repo;

import com.risingbee.realestate.automation.tenancy.domain.TenancyContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenancyContractRepository extends JpaRepository<TenancyContract, Long> {

    List<TenancyContract> findByOwnerAccountIdOrderByCreatedAtDesc(Long ownerAccountId);

    List<TenancyContract> findByTenantAccountIdOrderByCreatedAtDesc(Long tenantAccountId);

    @Query("SELECT tc FROM TenancyContract tc WHERE tc.propertyId = :propertyId AND tc.status = 'ACTIVE'")
    Optional<TenancyContract> findActiveContractForProperty(@Param("propertyId") Long propertyId);

    List<TenancyContract> findByOwnerAccountId(Long ownerAccountId);
    List<TenancyContract> findByTenantAccountId(Long tenantAccountId);
    
    


}