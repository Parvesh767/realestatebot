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
import com.risingbee.realestate.automation.service_hub.service.ServiceHubService;
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
	private final ServiceHubService serviceHubService;

	@Async // 🔑 Prevents blocking Meta's 3-second webhook timeout limit
	public void handleIncoming(Map<String, Object> payload) {

		if (!extractor.isUserMessageEvent(payload)) {
			return;
		}

		Optional<String> phoneOpt = extractor.extractPhone(payload);
		Optional<String> messageIdOpt = extractor.extractMessageId(payload);

		if (phoneOpt.isEmpty() || messageIdOpt.isEmpty()) {
			return;
		}

		String from = phoneOpt.get();
		String messageId = messageIdOpt.get();

		// 1️⃣ IDEMPOTENCY CHECK (Run before any processing)
		if (processedMessageRepository.existsByMessageId(messageId)) {
			log.info("Duplicate WhatsApp message ignored: {}", messageId);
			return;
		}
		processedMessageRepository.save(new ProcessedMessage(messageId));

		// Normalize input text safely
		String rawMessage = extractor.extractText(payload).orElse("");
		String cleanMessage = rawMessage.replace('\u00A0', ' ').trim();
		String upperMessage = cleanMessage.toUpperCase();

		// =========================================================================
		// ⚡ GLOBAL TECHNICIAN DISPATCH INTERCEPTOR 
		// (Allows field vendors to accept/complete jobs without requiring a broker account)
		// =========================================================================
		if (handleTechnicianCommands(from, cleanMessage, upperMessage)) {
			return; // Exit cleanly so chat bots never intercept job commands
		}

		// 2️⃣ STANDARD ACTOR RESOLUTION FOR CHATBOT FLOWS
		Optional<Actor> actorOpt = actorResolver.resolve(payload);
		if (actorOpt.isEmpty()) {
			log.warn("Could not resolve actor from WhatsApp payload for message: {}", cleanMessage);
			return;
		}

		Actor actor = actorOpt.get();
		withActorContext(actor, () -> handleIncomingWithActor(payload, actor, from, messageId, cleanMessage, upperMessage, messageIdOpt));
	}

	/**
	 * Intercepts ACCEPT and COMPLETE commands sent by vendors.
	 */
	private boolean handleTechnicianCommands(String fromPhone, String cleanMessage, String upperMessage) {
		try {
			if (upperMessage.startsWith("ACCEPT")) {
				String[] parts = cleanMessage.split("\\s+");
				if (parts.length >= 2) {
					Long bookingId = Long.parseLong(parts[1].replaceAll("[^0-9]", ""));
					log.info("Received ACCEPT command for Booking #{} from {}", bookingId, fromPhone);

					boolean accepted = serviceHubService.acceptJob(fromPhone, bookingId);
					if (accepted) {
						log.info("Job #{} successfully claimed by technician {}", bookingId, fromPhone);
					} else {
						log.warn("Job #{} could not be claimed by {}", bookingId, fromPhone);
					}
					return true;
				}
			}

			if (upperMessage.startsWith("COMPLETE")) {
				String[] parts = cleanMessage.split("\\s+");
				if (parts.length >= 3) {
					Long bookingId = Long.parseLong(parts[1].replaceAll("[^0-9]", ""));
					String otp = parts[2].trim();
					log.info("Received COMPLETE command for Booking #{} with OTP: {}", bookingId, otp);

					serviceHubService.completeJobWithOtp(bookingId, otp);
					log.info("Job #{} closed successfully with OTP", bookingId);
					return true;
				}
			}
		} catch (NumberFormatException nfe) {
			log.warn("Invalid formatting in technician command: '{}'", cleanMessage);
		} catch (Exception e) {
			log.error("Failed to process technician command from {}: {}", fromPhone, e.getMessage(), e);
			whatsAppSender.sendTextMessage(fromPhone, "❌ Command error: " + e.getMessage());
			return true;
		}
		return false;
	}

	private void handleIncomingWithActor(Map<String, Object> payload, Actor actor, String from, String messageId, 
										String cleanMessage, String upperMessage, Optional<String> messageIdOpt) {

		Optional<MediaInput> mediaOpt = extractor.extractImageMedia(payload);

		// Account Context Initialization
		Account account = accountService.getOrCreate(actor.externalId());
		actor = actor.withAccount(account);

		String lower = cleanMessage.toLowerCase();

		if (lower.equalsIgnoreCase("unlock") || lower.startsWith("unlock")) {
			handleLeadUnlockRequest(actor);
			return;
		}

		// Global Menu & Flow Chooser Trigger
		if (lower.equals("menu") || lower.equals("cancel") || lower.equals("start") || lower.equals("hi")) {
			BrokerConversation conv = conversationService.getOrCreate(account.getId());
			conv.startFlow(ConversationFlow.SEARCH, ConversationPhase.CHOOSING_FLOW);
			conversationService.save(conv);

			sendFlowChooser(actor.externalId());
			return;
		}

		// Quick Confirmation / Positive Intent
		if (isPositiveIntent(cleanMessage)) {
			yesHandler.handle(from, messageId);
			return;
		}

		// Stateful Flow Management
		BrokerConversation conversation = conversationService.getOrCreate(account.getId());

		if (conversation.getPhase() == ConversationPhase.CHOOSING_FLOW) {
			Optional<ConversationFlow> flowOpt = flowResolver.resolveExplicitChoice(cleanMessage);
			if (flowOpt.isEmpty()) {
				sendFlowChooser(from);
				return;
			}
			conversation.startFlow(flowOpt.get(), ConversationPhase.IN_FLOW);
			conversationService.save(conversation);
		}

		ParsedRequest parsed = SimpleParser.parse(cleanMessage);

		// Dynamic Intent Switcher (Onboarding → Search)
		if (conversation.getFlow() == ConversationFlow.BROKER_ONBOARDING && parsed.hasSearchIntent()) {
			log.info("Switching user intent from Onboarding to Search");

			conversation.startFlow(ConversationFlow.SEARCH, ConversationPhase.IN_FLOW);
			conversationService.save(conversation);

			whatsAppSender.sendTextMessage(actor.externalId(), "Got it! 👍 Switching over to property search.");

			searchHandler.handle(actor, parsed);
			return;
		}

		// Flow Execution
		switch (conversation.getFlow()) {
			case BROKER_ONBOARDING -> brokerOnboardingHandler.handle(actor, cleanMessage, messageIdOpt);
			case SEARCH -> searchHandler.handle(actor, parsed);
			case ADD_PROPERTY -> addPropertyHandler.handle(actor, cleanMessage, mediaOpt, messageIdOpt);
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
			String rechargeUrl;
			try {
				rechargeUrl = paymentService.createRechargePaymentLink(accountId, actor.externalId(), 999);
			} catch (Exception ex) {
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

	private String formatAmount(Number amount) {
		if (amount == null) {
			return "N/A";
		}
		return String.format("%,d", amount.longValue());
	}
}