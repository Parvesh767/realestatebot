package com.risingbee.realestate.automation.actor;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.enums.ActorType;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.parser.WhatsAppPayloadExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActorResolver {

    private final WhatsAppPayloadExtractor extractor;
    private final AccountRepository accountRepository;

    /**
     * Resolve Actor from incoming WhatsApp payload.
     * Identity-only. No business logic. No enforcement.
     */
    public Optional<Actor> resolve(Map<String, Object> payload) {

        Optional<String> phoneOpt = extractor.extractPhone(payload);

        if (phoneOpt.isEmpty()) {
            log.warn("ActorResolver: no phone found in payload");
            return Optional.empty();
        }

        String phone = phoneOpt.get();

        Optional<Account> accountOpt = accountRepository.findByPhone(phone);

        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();

            Actor actor = new Actor(
                    account.getType(),   // USER / OWNER / BROKER
                    phone,
                    account.getId()
            );

            log.debug("ActorResolver: resolved ACCOUNT → {}", actor);
            return Optional.of(actor);
        }

        // No account yet → USER by default
        Actor actor = new Actor(
                ActorType.USER,
                phone,
                null
        );

        log.debug("ActorResolver: resolved USER (no account) → {}", actor);
        return Optional.of(actor);
    }
}
