package com.risingbee.realestate.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.service.AccountService;
import com.risingbee.realestate.automation.domain.BrokerConversation;
import com.risingbee.realestate.automation.repo.BrokerConversationRepository;
import com.risingbee.realestate.automation.service.BrokerAreaService;
import com.risingbee.realestate.automation.service.BrokerPreferenceService;
import com.risingbee.realestate.automation.service.BrokerService;
import com.risingbee.realestate.automation.service.WhatsAppSender;
import com.risingbee.realestate.enums.BrokerOnboardingStep;
import com.risingbee.realestate.flow.ConversationFlow;
import com.risingbee.realestate.lifecycle.ConversationLifecycleManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerOnboardingHandler {

	private final ConversationLifecycleManager lifecycle;
	private final BrokerConversationRepository conversationRepo;
	private final AccountService accountService;
	private final BrokerService brokerService;
	private final BrokerAreaService brokerAreaService;
	private final BrokerPreferenceService brokerPreferenceService;

	private final WhatsAppSender sender;

	public void handle(Actor actor, String message, Optional<String> messageId) {

	    if (actor.internalId() == null) {
	        sender.sendTextMessage(
	            actor.externalId(),
	            "Please verify your account before onboarding."
	        );
	        return;
	    }

	    Optional<BrokerConversation> convOpt = lifecycle.acquire(
	        actor.internalId(),
	        ConversationFlow.BROKER_ONBOARDING,
	        messageId,
	        actor.externalId()
	    );

	    log.warn(
	    		"lifecycle created {} ",convOpt	          
	    		);
	    
	    if (convOpt.isEmpty()) return;
	    

	    BrokerConversation conv = convOpt.get();

	    BrokerOnboardingStep step = conv.getOnboardingStep();

	    if (step == null) {
	        log.warn(
	            "Conversation {} has null onboarding step. Resetting to ASK_AREAS.",
	            conv.getId()
	        );
	        step = BrokerOnboardingStep.ASK_AREAS;
	        conv.setOnboardingStep(step);
	        conversationRepo.save(conv);
	    }

	    switch (step) {
	        case ASK_AREAS  -> handleAreas(conv, actor, message);
	        case ASK_BUDGET -> handleBudget(conv, actor, message);
	        case ASK_BHK    -> handleBhk(conv, actor, message);
	        case CONFIRM    -> handleConfirm(conv, actor);
	    }
	}


	/* ---------------- STEPS ---------------- */

	private void handleAreas(BrokerConversation conv, Actor actor, String message) {
		if (message == null || message.length() < 3) {
			send(actor, "Please enter valid areas (comma separated).");
			return;
		}

		List<String> areas = List.of(message.split(",")).stream().map(String::trim).filter(s -> !s.isBlank()).toList();

		boolean saved = brokerAreaService.save(actor, areas);

		if (!saved) {
			send(actor, """
					❌ I couldn't recognize those areas.

					Example:
					DLF Phase 3, Sector 22
					""");
			return;
		}

		conv.put("areas", areas);
		conv.setOnboardingStep(BrokerOnboardingStep.ASK_BUDGET);
		conversationRepo.save(conv);

		send(actor, """
				💰 What rental budget do you handle?

				Examples:
				20k-30k
				25k
				""");
	}

	private void handleBudget(BrokerConversation conv, Actor actor, String message) {
		Integer min;
		Integer max;

		try {
			String cleaned = message.replaceAll("\\s+", "").toLowerCase();

			if (cleaned.contains("-")) {
				String[] parts = cleaned.split("-");
				min = parseAmount(parts[0]);
				max = parseAmount(parts[1]);
			} else {
				min = parseAmount(cleaned);
				max = min;
			}
		} catch (Exception e) {
			send(actor, "Invalid budget format. Example: 20k-30k");
			return;
		}

		conv.put("minBudget", min);
		conv.put("maxBudget", max);

		conv.setOnboardingStep(BrokerOnboardingStep.ASK_BHK);
		conversationRepo.save(conv);

		send(actor, """
				🏠 Which property types do you deal in?

				Example:
				1BHK, 2BHK, 3BHK
				""");
	}

	@SuppressWarnings("unchecked")
	private void handleBhk(BrokerConversation conv, Actor actor, String message) {
		if (!message.toLowerCase().contains("bhk")) {
			send(actor, "Please reply like: 1BHK, 2BHK");
			return;
		}

		Set<String> bhks = Set.of(message.toUpperCase().split(","));

		conv.put("bhks", bhks);
		conv.setOnboardingStep(BrokerOnboardingStep.CONFIRM);
		conversationRepo.save(conv);

		send(actor, buildPreview(conv));
		send(actor, "✅ Type *CONFIRM* to finish onboarding");
	}

	private void handleConfirm(BrokerConversation conv, Actor actor) {
//		@SuppressWarnings("unchecked")
//		List<String> areas = Optional.ofNullable(conv.get("areas", List.class)).orElse(new ArrayList<>());
		
		Set<String> areas = conv.getStringSet("areas");

		Integer min = conv.get("minBudget", Integer.class);
		Integer max = conv.get("maxBudget", Integer.class);

		@SuppressWarnings("unchecked")
		Set<String> bhks = Optional.ofNullable(conv.get("bhks", Set.class)).orElse(Set.of());

		brokerPreferenceService.updateBudget(actor, min, max);

		brokerPreferenceService.updateBhks(actor, new ArrayList<>(bhks));
		
		accountService.upgradeToBroker(actor);

		brokerService.activateBroker(actor);

		conversationRepo.delete(conv);

		send(actor, """
				✅ You're all set!

				You will now start receiving tenant enquiries.
				""");
	}

	/* ---------------- HELPERS ---------------- */

	private String buildPreview(BrokerConversation conv) {
		return """
				🧾 *Onboarding Summary*

				• Areas: %s
				• Budget: %s - %s
				• Property Types: %s

				Confirm to continue.
				""".formatted(conv.get("areas", List.class), conv.get("minBudget", Integer.class),
				conv.get("maxBudget", Integer.class), conv.get("bhks", Set.class));
	}

	private void send(Actor actor, String message) {
		sender.sendTextMessage(actor.externalId(), message);
	}

	private Integer parseAmount(String value) {
		value = value.toLowerCase();
		if (value.endsWith("k")) {
			return Integer.parseInt(value.replace("k", "")) * 1000;
		}
		return Integer.parseInt(value);
	}
}
