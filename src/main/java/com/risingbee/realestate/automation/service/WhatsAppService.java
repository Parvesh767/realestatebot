package com.risingbee.realestate.automation.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.ActorResolver;
import com.risingbee.realestate.automation.actor.enums.Capability;
import com.risingbee.realestate.automation.actor.repo.AccountRepository;
import com.risingbee.realestate.automation.domain.BrokerConversation;
import com.risingbee.realestate.automation.domain.ProcessedMessage;
import com.risingbee.realestate.automation.dto.MediaInput;
import com.risingbee.realestate.automation.interfaces.BrokerOnboardingService;
import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.parser.SimpleParser;
import com.risingbee.realestate.automation.parser.WhatsAppPayloadExtractor;
import com.risingbee.realestate.automation.repo.ProcessedMessageRepository;
import com.risingbee.realestate.flow.ConversationFlow;
import com.risingbee.realestate.flow.ConversationPhase;
import com.risingbee.realestate.flow.FlowResolver;
import com.risingbee.realestate.flow.convService.ConversationService;
import com.risingbee.realestate.handler.AddPropertyHandler;
import com.risingbee.realestate.handler.SearcherSearchHandler;
import com.risingbee.realestate.handler.SearcherYesHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final AccountRepository accountRepository;

	private final WhatsAppSender whatsAppSender;

	private final ProcessedMessageRepository processedMessageRepository;

	private final WhatsAppPayloadExtractor extractor;
	private final AddPropertyHandler addPropertyHandler;
	private final SearcherSearchHandler searchHandler;
	private final SearcherYesHandler yesHandler;
	private final BrokerOnboardingService brokerOnboardingService;
	private final ConversationService conversationService;
	private final FlowResolver flowResolver;
	private final ActorResolver actorResolver;

    WhatsAppService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

//	public void handleIncoming(Map<String, Object> payload) {
//
//		// 0️⃣ Ignore non-user events
//		if (!extractor.isUserMessageEvent(payload)) {
//			return;
//		}
//		
//		Optional<String> phoneOpt = extractor.extractPhone(payload);
//		Optional<String> messageIdOpt = extractor.extractMessageId(payload);
//
//		if (phoneOpt.isEmpty() || messageIdOpt.isEmpty()) {
//			return;
//		}
//
//		String from = phoneOpt.get();
//		String messageId = messageIdOpt.get();
//
//		String message = extractor.extractText(payload).map(String::trim).orElse("");
//
//		Optional<MediaInput> mediaOpt = extractor.extractImageMedia(payload);
//
//		// 1️⃣ HARD idempotency (keep old guarantee)
//		if (processedMessageRepository.existsByMessageId(messageId)) {
//			log.info("Duplicate WhatsApp message ignored: {}", messageId);
//			return;
//		}
//		processedMessageRepository.save(new ProcessedMessage(messageId));
//
//		
//		// 2️⃣ Resolve Actor (observe-only)
//		Optional<Actor> actorOpt = actorResolver.resolve(payload);
//
//		if (actorOpt.isEmpty()) {
//		    log.warn("Could not resolve actor from payload, skipping message");
//		    return;
//		}
//
//		Actor actor = actorOpt.get();
//		ActorContext.set(actor);
//
//		log.info("Incoming WhatsApp message from Actor → {}", actor);
//		
//		
//		
//		// 2️⃣ YES short-circuit (unchanged)
//		if (message.equalsIgnoreCase("yes")) {
//			yesHandler.handle(from, messageId);
//			return;
//		}
//
//
//		Broker broker = null;
//
//		if (actor != null && actor.type() == ActorType.BROKER) {
//		    broker = brokerRepository.findById(actor.internalId())
//		            .orElse(null);
//
//		    if (broker == null) {
//		        log.warn(
//		            "ActorContext says BROKER {} but broker not found in DB",
//		            actor
//		        );
//		    }
//		}
//
//		// 🔁 Fallback to old behavior (unchanged semantics)
//		if (broker == null) {
//		    broker = brokerRepository.findByPhone(from)
//		            .orElseGet(() -> brokerService.createForOnboarding(from));
//		}
//		
//		log.debug(
//			    "Resolved broker {} for actor {} via {}",
//			    broker.getId(),
//			    actor,
//			    (actor != null && actor.type() == ActorType.BROKER) ? "ActorContext" : "legacy lookup"
//			);
//
//		
//		
////		// 3️⃣ Resolve or create Broker ONLY as identity
////		Broker broker = brokerRepository.findByPhone(from).orElseGet(() -> brokerService.createForOnboarding(from));
//
//		// 4️⃣ Conversation = SINGLE source of truth for flow
//		BrokerConversation conversation = conversationService.getOrCreate(broker.getId());
//
//		// 5️⃣ Conversation-level idempotency (extra safety)
//		if (!conversation.markMessageProcessed(messageId)) {
//			return;
//		}
//
//		// 1️⃣ Flow selection phase
//		if (conversation.getPhase() == ConversationPhase.CHOOSING_FLOW) {
//
//			Optional<ConversationFlow> flow = flowResolver.resolveExplicitChoice(message);
//
//			if (flow.isEmpty()) {
//				sendFlowChooser(from);
//				return;
//			}
//
//			conversation.setFlow(flow.get());
//			conversation.setPhase(ConversationPhase.IN_FLOW);
//			conversationService.save(conversation);
//		}
//		
//		
//		log.info(
//			    "Actor {} entering conversation flow={}, phase={}",
//			    actor,
//			    conversation.getFlow(),
//			    conversation.getPhase()
//			);
//
//		switch (conversation.getFlow()) {
//
//			case BROKER_ONBOARDING -> {
//				// 🔑 Resolve Broker (domain object)
//				broker = brokerRepository.findById(conversation.getBrokerId())
//						.orElseThrow(() -> new IllegalStateException("Broker not found"));
//	
//				BrokerContext.set(broker);
//				try {
//					brokerOnboardingService.handle(broker, message);
//				} finally {
//					
//				    ActorContext.clear();
//					BrokerContext.clear();
//				}
//			}
//	
//			case SEARCH -> {
//				// 🔑 Parse intent (search handler contract)
//				ParsedRequest parsed = SimpleParser.parse(message);
//	
//				searchHandler.handle(from, // or `from`
//						parsed);
//			}
//	
//			case ADD_PROPERTY -> {
//				addPropertyHandler.handle(broker.getPhone(), message, mediaOpt, messageIdOpt);
//			}
//		}
//
//	}
//	

	public void handleIncoming(Map<String, Object> payload) {

		// 0️⃣ Ignore non-user events
		if (!extractor.isUserMessageEvent(payload)) {
			return;
		}

		Optional<Actor> actorOpt = actorResolver.resolve(payload);
		if (actorOpt.isEmpty()) {
			log.warn("Could not resolve actor from payload");
			return;
		}

		Actor actor = actorOpt.get();

		withActorContext(actor, () -> {
			handleIncomingWithActor(payload, actor);
		});
	}

	private void handleIncomingWithActor(Map<String, Object> payload, Actor actor) {

		Optional<String> phoneOpt = extractor.extractPhone(payload);
		Optional<String> messageIdOpt = extractor.extractMessageId(payload);

		if (phoneOpt.isEmpty() || messageIdOpt.isEmpty()) {
			return;
		}

		String from = phoneOpt.get();
		String messageId = messageIdOpt.get();
		String message = extractor.extractText(payload).map(String::trim).orElse("");
		Optional<MediaInput> mediaOpt = extractor.extractImageMedia(payload);

		// 1️⃣ HARD idempotency
		if (processedMessageRepository.existsByMessageId(messageId)) {
			log.info("Duplicate WhatsApp message ignored: {}", messageId);
			return;
		}
		processedMessageRepository.save(new ProcessedMessage(messageId));

		log.info("Incoming WhatsApp message from Actor → {}", actor);

		// 2️⃣ YES short-circuit
		if (message.equalsIgnoreCase("yes")) {
			yesHandler.handle(from, messageId);
			return;
		}

		// Stateless handling for actors without internal identity
		if (actor.internalId() == null) {

		    Optional<ConversationFlow> flowOpt =
		        flowResolver.resolveExplicitChoice(message);

		    // SEARCH stays stateless
		    if (flowOpt.isEmpty() || flowOpt.get() == ConversationFlow.SEARCH) {
		        ParsedRequest parsed = SimpleParser.parse(message);
		        searchHandler.handle(actor, parsed);
		        return;
		    }

		    // 🔑 NATURAL LANGUAGE ONBOARDING TRIGGER
		    if (flowOpt.get() == ConversationFlow.BROKER_ONBOARDING) {

		        // ✅ CREATE ACCOUNT IMMEDIATELY
		        Account account =
		            accountService.createFromActor(actor);

		        // ✅ Upgrade actor
		        Actor onboardedActor =
		            actor.withInternalId(account.getId())
		                 .withType(account.getType());

		        ActorContext.set(onboardedActor);

		        // ✅ Create conversation NOW (owner_account_id will NOT be null)
		        BrokerConversation conversation =
		            conversationService.getOrCreate(account.getId());

		        conversation.setFlow(ConversationFlow.BROKER_ONBOARDING);
		        conversation.setPhase(ConversationPhase.IN_FLOW);
		        conversationService.save(conversation);

		        // ✅ Hand off to onboarding
		        brokerOnboardingService.handle(onboardedActor, message);

		        return;
		    }

		    // Fallback (should rarely happen)
		    whatsAppSender.sendTextMessage(
		        actor.externalId(),
		        """
		        👋 You can search for properties freely.
		        To list properties, just say:
		        *I want to list my property*
		        """
		    );
		    return;
		}


		// 4️⃣ Conversation
		BrokerConversation conversation = conversationService.getOrCreate(actor.internalId());

		if (!conversation.markMessageProcessed(messageId)) {
			return;
		}

		if (conversation.getPhase() == ConversationPhase.CHOOSING_FLOW) {
			Optional<ConversationFlow> flow = flowResolver.resolveExplicitChoice(message);

			if (flow.isEmpty()) {
				sendFlowChooser(from);
				return;
			}

			conversation.setFlow(flow.get());
			conversation.setPhase(ConversationPhase.IN_FLOW);
			conversationService.save(conversation);
		}

		log.info("Actor {} entering conversation flow={}, phase={}", actor, conversation.getFlow(),
				conversation.getPhase());

		if (conversation.getFlow() == ConversationFlow.ADD_PROPERTY
				&& !ActorContext.hasCapability(Capability.ADD_PROPERTY)) {

			log.warn("Actor {} blocked from ADD_PROPERTY flow", actor);

			whatsAppSender.sendTextMessage(actor.externalId(), """
					🚫 *You can’t add properties yet*

					This action requires property-listing access.

					👉 If you are a broker or owner, reply:
					*I want to list my property*
					""");
			return;
		}

		switch (conversation.getFlow()) {

		case BROKER_ONBOARDING -> {
			brokerOnboardingService.handle(ActorContext.get(), message);
		}

		case SEARCH -> {
			ParsedRequest parsed = SimpleParser.parse(message);
			searchHandler.handle(ActorContext.get(), parsed);
		}

		case ADD_PROPERTY -> {
			addPropertyHandler.handle(ActorContext.get(), message, mediaOpt, messageIdOpt);
		}
		}
	}

	private void sendFlowChooser(String from) {
		whatsAppSender.sendTextMessage(from, """
				👋 Hi!

				What would you like to do?

				1️⃣ Find a property
				2️⃣ I am a broker / agent
				""");
	}

	private void withActorContext(Actor actor, Runnable action) {
		ActorContext.set(actor);
		try {
			action.run();
		} finally {
			ActorContext.clear();
		}
	}

}
