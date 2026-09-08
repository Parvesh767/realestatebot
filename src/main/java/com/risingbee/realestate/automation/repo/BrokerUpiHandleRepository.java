package com.risingbee.realestate.automation.repo;

import com.risingbee.realestate.automation.domain.BrokerUpiHandle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BrokerUpiHandleRepository extends JpaRepository<BrokerUpiHandle, Long> {
    List<BrokerUpiHandle> findByAccountId(Long accountId);
    Optional<BrokerUpiHandle> findByAccountIdAndIsDefaultTrue(Long accountId);
}