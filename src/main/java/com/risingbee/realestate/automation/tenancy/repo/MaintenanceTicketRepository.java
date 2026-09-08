package com.risingbee.realestate.automation.tenancy.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.risingbee.realestate.automation.tenancy.domain.MaintenanceTicket;

@Repository
public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {
    List<MaintenanceTicket> findByTenancyContractIdOrderByCreatedAtDesc(Long contractId);
    List<MaintenanceTicket> findByTenantAccountIdOrderByCreatedAtDesc(Long tenantAccountId);
}