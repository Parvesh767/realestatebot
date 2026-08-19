package com.risingbee.realestate.lifecycle;


import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.domain.BrokerConversation;
import com.risingbee.realestate.automation.repo.BrokerConversationRepository;
import com.risingbee.realestate.flow.ConversationFlow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationLifecycleManager {

    private static final Duration TIMEOUT = Duration.ofMinutes(15);

    private final BrokerConversationRepository conversationRepo;

    public Optional<BrokerConversation> acquire(
        Long ownerAccountId,
        ConversationFlow flow,
        Optional<String> messageId,
        String externalId
    ) {

        BrokerConversation conv =
            conversationRepo
                .findByOwnerAccountIdAndFlow(ownerAccountId, flow)
                .orElseGet(() -> create(ownerAccountId, flow));
      
        
        // 1️⃣ Expiry check
        if (isExpired(conv)) {
            conversationRepo.delete(conv);

            log.info(
                "Conversation expired for account={}, flow={}",
                ownerAccountId,
                flow
            );
            
         

            return Optional.of(conv);
        }

        // 2️⃣ Idempotency check
//        if (messageId.isPresent()) {
//            boolean fresh =
//                conv.markMessageProcessed(messageId.get());
//
//            if (!fresh) {
//                log.info(
//                    "Duplicate message ignored: {}",
//                    messageId.get()
//                );
//                return Optional.empty();
//            }
//        }

        conversationRepo.save(conv);
        return Optional.of(conv);
    }

    private boolean isExpired(BrokerConversation conv) {
        return conv.getUpdatedAt()
            .isBefore(Instant.now().minus(TIMEOUT));
    }

    private BrokerConversation create(
        Long ownerAccountId,
        ConversationFlow flow
    ) {
        BrokerConversation conv =
            BrokerConversation.createForAccount(ownerAccountId);

        return conversationRepo.save(conv);
    }
}
