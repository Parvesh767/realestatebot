package com.risingbee.realestate.automation.actor.repo;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.risingbee.realestate.automation.actor.domain.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByPhone(String phone);
}

