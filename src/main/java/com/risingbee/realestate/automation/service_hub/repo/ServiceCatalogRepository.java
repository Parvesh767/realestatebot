package com.risingbee.realestate.automation.service_hub.repo;

import com.risingbee.realestate.automation.service_hub.domain.ServiceCatalog;
import com.risingbee.realestate.automation.service_hub.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long> {
    List<ServiceCatalog> findByActiveTrueOrderByCategoryAsc();
    List<ServiceCatalog> findByCategoryAndActiveTrue(ServiceCategory category);
}