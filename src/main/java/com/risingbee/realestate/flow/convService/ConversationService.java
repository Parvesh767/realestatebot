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

    /**
     * Start a flow on an existing conversation.
     * Creation is handled by ConversationLifecycleManager.
     */
    public void startFlow(
        BrokerConversation conversation,
        ConversationFlow flow
    ) {
        if (conversation.getFlow() != null) {
            return; // already in a flow
        }

        conversation.setFlow(flow);
        repo.save(conversation);
    }

    /**
     * Reset conversation back to flow selection.
     */
    public void reset(BrokerConversation conversation) {
        conversation.setFlow(null);
        conversation.setStep(null);
        conversation.clearProcessedMessages();
        repo.save(conversation);
    }

    public void save(BrokerConversation conversation) {
        repo.save(conversation);
    }
    
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

}
