package com.risingbee.realestate.automation.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.ActorResolver;
import com.risingbee.realestate.automation.actor.domain.Account;
import com.risingbee.realestate.automation.actor.service.AccountService;
import com.risingbee.realestate.automation.domain.BrokerConversation;
import com.risingbee.realestate.automation.domain.Lead;
import com.risingbee.realestate.automation.domain.ProcessedMessage;
import com.risingbee.realestate.automation.dto.LeadResponseDTO;
import com.risingbee.realestate.automation.dto.MediaInput;
import com.risingbee.realestate.automation.exception.InsufficientCreditsException;
import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.parser.SimpleParser;
import com.risingbee.realestate.automation.parser.WhatsAppPayloadExtractor;
import com.risingbee.realestate.automation.repo.ProcessedMessageRepository;
import com.risingbee.realestate.flow.ConversationFlow;
import com.risingbee.realestate.flow.ConversationPhase;
import com.risingbee.realestate.flow.FlowResolver;
import com.risingbee.realestate.flow.convService.ConversationService;
import com.risingbee.realestate.handler.AddPropertyHandler;
import com.risingbee.realestate.handler.BrokerOnboardingHandler;
import com.risingbee.realestate.handler.SearcherSearchHandler;
import com.risingbee.realestate.handler.SearcherYesHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

	private final AccountService accountService;
	private final WhatsAppSender whatsAppSender;
	private final ProcessedMessageRepository processedMessageRepository;
	private final WhatsAppPayloadExtractor extractor;
	private final AddPropertyHandler addPropertyHandler;
	private final SearcherSearchHandler searchHandler;
	private final SearcherYesHandler yesHandler;
	private final ConversationService conversationService;
	private final FlowResolver flowResolver;
	private final ActorResolver actorResolver;
	private final BrokerOnboardingHandler brokerOnboardingHandler;
	private final LeadService leadService;
	private final LeadPaywallService leadPaywallService;
	private final PaymentService paymentService;

	@Async // 🔑 Prevents blocking Meta's 3-second webhook timeout limit
	public void handleIncoming(Map<String, Object> payload) {

		if (!extractor.isUserMessageEvent(payload)) {
			return;
		}

		Optional<Actor> actorOpt = actorResolver.resolve(payload);
		if (actorOpt.isEmpty()) {
			log.warn("Could not resolve actor from WhatsApp payload");
			return;
		}

		Actor actor = actorOpt.get();

		withActorContext(actor, () -> handleIncomingWithActor(payload, actor));
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

		// 1️⃣ IDEMPOTENCY CHECK (Run before standard DB writes)
		if (processedMessageRepository.existsByMessageId(messageId)) {
			log.info("Duplicate WhatsApp message ignored: {}", messageId);
			return;
		}
		processedMessageRepository.save(new ProcessedMessage(messageId));

		// 2️⃣ Account Context Initialization
		Account account = accountService.getOrCreate(actor.externalId());
		actor = actor.withAccount(account);

		// Inside handleIncomingWithActor(...) in WhatsAppService.java

		String lower = message.toLowerCase().trim();

		if (lower.equalsIgnoreCase("unlock") || lower.startsWith("unlock")) {
			handleLeadUnlockRequest(actor);
			return;
		}

		// 3️⃣ Global Menu & Flow Chooser Trigger
		if (lower.equals("menu") || lower.equals("cancel") || lower.equals("start") || lower.equals("hi")) {
			BrokerConversation conv = conversationService.getOrCreate(account.getId());
			conv.startFlow(ConversationFlow.SEARCH, ConversationPhase.CHOOSING_FLOW);
			conversationService.save(conv);

			sendFlowChooser(actor.externalId());
			return;
		}

		// 4️⃣ Quick Confirmation / Positive Intent
		if (isPositiveIntent(message)) {
			yesHandler.handle(from, messageId);
			return;
		}

		// 5️⃣ Stateful Flow Management
		BrokerConversation conversation = conversationService.getOrCreate(account.getId());

		if (conversation.getPhase() == ConversationPhase.CHOOSING_FLOW) {
			Optional<ConversationFlow> flowOpt = flowResolver.resolveExplicitChoice(message);
			if (flowOpt.isEmpty()) {
				sendFlowChooser(from);
				return;
			}
			conversation.startFlow(flowOpt.get(), ConversationPhase.IN_FLOW);
			conversationService.save(conversation);
		}

		ParsedRequest parsed = SimpleParser.parse(message);

		// 6️⃣ Dynamic Intent Switcher (Onboarding → Search)
		if (conversation.getFlow() == ConversationFlow.BROKER_ONBOARDING && parsed.hasSearchIntent()) {
			log.info("Switching user intent from Onboarding to Search");

			conversation.startFlow(ConversationFlow.SEARCH, ConversationPhase.IN_FLOW);
			conversationService.save(conversation);

			whatsAppSender.sendTextMessage(actor.externalId(), "Got it! 👍 Switching over to property search.");

			searchHandler.handle(actor, parsed);
			return;
		}

		// 7️⃣ Flow Execution
		switch (conversation.getFlow()) {
		case BROKER_ONBOARDING -> brokerOnboardingHandler.handle(actor, message, messageIdOpt);
		case SEARCH -> searchHandler.handle(actor, parsed);
		case ADD_PROPERTY -> addPropertyHandler.handle(actor, message, mediaOpt, messageIdOpt);
		default -> searchHandler.handle(actor, parsed);
		}
	}

	private boolean isPositiveIntent(String msg) {
		return List.of("yes", "haan", "ok", "interested", "y").contains(msg.toLowerCase().trim());
	}

	private void sendFlowChooser(String recipient) {
		whatsAppSender.sendTextMessage(recipient, """
				👋 Welcome!

				How can we assist you today?
				1️⃣ Find a property
				2️⃣ List a property / Broker services
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

	private void handleLeadUnlockRequest(Actor actor) {
		Long accountId = actor.internalId();
		if (accountId == null) {
			whatsAppSender.sendTextMessage(actor.externalId(),
					"❌ Account identity not found. Please type *START* to register.");
			return;
		}

		// Fetch latest locked lead for this broker
		Optional<Lead> latestLeadOpt = leadService.findLatestLockedLeadForOwner(accountId);

		if (latestLeadOpt.isEmpty()) {
			whatsAppSender.sendTextMessage(actor.externalId(), "ℹ️ You have no pending locked leads!");
			return;
		}

		Lead lead = latestLeadOpt.get();

		try {
			// Unlock lead and deduct 1 credit
			LeadResponseDTO unlockedDTO = leadPaywallService.unlockLeadForBroker(actor, lead.getId());

			whatsAppSender.sendTextMessage(actor.externalId(), """
					🔓 *LEAD UNLOCKED SUCCESSFULLY!*

					📱 *Full Buyer Phone:* %s
					🛏 *Requirement:* %s
					💰 *Budget:* Up to ₹%s

					⚡ _1 Credit deducted from your account._
					""".formatted(unlockedDTO.phoneNumber(), unlockedDTO.bhk() != null ? unlockedDTO.bhk() : "Any BHK",
					formatAmount(unlockedDTO.maxBudget())));

		} catch (InsufficientCreditsException e) {
			// 🔥 FIX: Generate and include the recharge payment link here
			String rechargeUrl;
			try {
				// If you have PaymentService wired:
				rechargeUrl = paymentService.createRechargePaymentLink(accountId, actor.externalId(), 999);
			} catch (Exception ex) {
				// Fallback direct URL
				rechargeUrl = "https://yourdomain.com/recharge?account=" + accountId;
			}

			whatsAppSender.sendTextMessage(actor.externalId(), """
					🚫 *INSUFFICIENT CREDITS!*

					You have 0 credits remaining to unlock this buyer lead.

					💳 *Top up 30 Credits for ₹999 via UPI/Card:*
					%s

					⚡ _Credits are added automatically within 5 seconds of payment!_
					""".formatted(rechargeUrl));

		} catch (Exception e) {
			log.error("Failed to unlock lead id={} for actor={}", lead.getId(), actor, e);
			whatsAppSender.sendTextMessage(actor.externalId(),
					"❌ Something went wrong while unlocking the lead. Please try again later.");
		}
	}

	/**
	 * Safely formats numerical amounts (Long, Integer, Double) into standard
	 * comma-separated format. E.g., 15000 -> "15,000" or 15000000 -> "15,000,000"
	 */
	private String formatAmount(Number amount) {
		if (amount == null) {
			return "N/A";
		}
		return String.format("%,d", amount.longValue());
	}

}