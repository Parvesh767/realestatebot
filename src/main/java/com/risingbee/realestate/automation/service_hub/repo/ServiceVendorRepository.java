package com.risingbee.realestate.automation.service_hub.repo;

import com.risingbee.realestate.automation.service_hub.domain.ServiceVendor;
import com.risingbee.realestate.automation.service_hub.enums.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServiceVendorRepository extends JpaRepository<ServiceVendor, Long> {
    List<ServiceVendor> findBySkillCategoryAndOperationalCityCodeAndActiveTrue(ServiceCategory skill, String city);
    Optional<ServiceVendor> findByPhone(String phone);
	List<ServiceVendor> findAllByPhone(String rawPhone);
}