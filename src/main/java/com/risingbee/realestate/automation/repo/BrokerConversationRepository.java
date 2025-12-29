package com.risingbee.realestate.automation.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.risingbee.realestate.automation.domain.BrokerConversation;
import com.risingbee.realestate.flow.ConversationFlow;

public interface BrokerConversationRepository
extends JpaRepository<BrokerConversation, Long> {

Optional<BrokerConversation> findByBrokerIdAndFlow(
    Long brokerId,
    ConversationFlow flow
);

void deleteByBrokerIdAndFlow(
    Long brokerId,
    ConversationFlow flow
);

Optional<BrokerConversation> findActiveByBrokerId(Long id);
}

