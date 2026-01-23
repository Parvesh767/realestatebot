package com.risingbee.realestate.automation.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.risingbee.realestate.automation.domain.BrokerConversation;
import com.risingbee.realestate.flow.ConversationFlow;

public interface BrokerConversationRepository
        extends JpaRepository<BrokerConversation, Long> {

    Optional<BrokerConversation> findByOwnerAccountIdAndFlow(
        Long ownerAccountId,
        ConversationFlow flow
    );


    Optional<BrokerConversation> findByOwnerAccountId(Long ownerAccountId);

//    Optional<BrokerConversation> findByOwnerAccountIdAndFlow(
//        Long ownerAccountId,
//        ConversationFlow flow
//    );
//}
}
