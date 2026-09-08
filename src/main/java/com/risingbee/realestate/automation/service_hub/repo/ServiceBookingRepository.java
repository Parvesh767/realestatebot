package com.risingbee.realestate.automation.service_hub.repo;

import com.risingbee.realestate.automation.service_hub.domain.ServiceBooking;
import com.risingbee.realestate.automation.service_hub.enums.BookingStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, Long> {
    List<ServiceBooking> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    List<ServiceBooking> findByAssignedVendorIdOrderByCreatedAtDesc(Long vendorId);
    
    
    
    // Find active bookings for the tenant's contract
    List<ServiceBooking> findByTenancyContractIdAndStatusInOrderByCreatedAtDesc(
            Long tenancyContractId, 
            List<BookingStatus> statuses
    );
    
}