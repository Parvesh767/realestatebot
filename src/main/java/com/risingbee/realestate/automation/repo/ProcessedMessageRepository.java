package com.risingbee.realestate.automation.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.risingbee.realestate.automation.domain.ProcessedMessage;

public interface ProcessedMessageRepository
extends JpaRepository<ProcessedMessage, Long> {

boolean existsByMessageId(String messageId);
}
