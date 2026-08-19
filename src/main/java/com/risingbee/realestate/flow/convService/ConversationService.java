package com.risingbee.realestate.flow.convService;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.domain.BrokerConversation;
import com.risingbee.realestate.automation.repo.BrokerConversationRepository;
import com.risingbee.realestate.flow.ConversationFlow;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {

    private final BrokerConversationRepository repo;

    public BrokerConversation getOrCreate(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException(
                "Cannot create conversation without accountId"
            );
        }

        return repo.findByOwnerAccountId(accountId)
            .orElseGet(() ->
                repo.save(BrokerConversation.createForAccount(accountId))
            );
    }

    public void save(BrokerConversation conversation) {
        repo.save(conversation);
    }

    public void delete(BrokerConversation conversation) {
        repo.delete(conversation);
    }
}
