package com.risingbee.realestate.automation.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.enums.ActorType;
import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.interfaces.BrokerOnboardingService;
import com.risingbee.realestate.automation.repo.BrokerRepository;
import com.risingbee.realestate.enums.BrokerOnboardingStep;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerOnboardingServiceImpl implements BrokerOnboardingService {

	private final BrokerRepository brokerRepository;
	private final BrokerAreaService brokerAreaService;
	private final BrokerPreferenceService brokerPreferenceService;
	private final WhatsAppSender whatsAppSender;

	@Override
	@Transactional
	public void handle(Actor actor, String message)
{
		if (actor == null || message == null) return;

		if (actor.type() != ActorType.BROKER) {
		    log.warn("Actor {} attempted broker onboarding", actor);
		    return;
		}

		Broker broker = brokerRepository
		        .findById(actor.internalId())
		        .orElseThrow(() ->
		            new IllegalStateException("Broker not found for actor " + actor)
		        );
		
		if (broker.getOnboardingStep() == BrokerOnboardingStep.DONE) {
		    log.info("Broker {} already onboarded, ignoring onboarding input", broker.getId());
		    return;
		}

		
		// 🛡️ HARD GUARD: heal invalid brokers
		if (broker.getOnboardingStep() == null) {
			log.warn("Broker {} has NULL onboardingStep. Auto-healing to START.", broker.getId());

			broker.resetOnboarding();
			brokerRepository.save(broker);
		}

		String text = message.trim().toLowerCase();

		switch (broker.getOnboardingStep()) {

		case START -> askLocation(broker);

		case AREAS -> handleAreas(broker, text);

		case BUDGET -> handleBudget(broker, text);

		case BHK -> handleBhk(broker, text);

		case DONE -> {
			broker.activate();
			brokerRepository.save(broker);
		}
		}
	}

	/* ---------------- STEP HANDLERS ---------------- */

	private void askLocation(Broker broker) {

		broker.advanceOnboarding(BrokerOnboardingStep.AREAS);
		send(broker, "Got it — Gurugram 👍\n\nYou can also specify localities like DLF Phase 3 or Sector 22.");

	}

	private void handleAreas(Broker broker, String text) {

		boolean saved = brokerAreaService.save(broker.getId(), List.of(text.split(",")));

		if (!saved) {
			send(broker, """
					❌ I couldn't recognize that area.

					Please reply with a valid location.
					Example:
					Gurgaon, DLF Phase 3, Sector 22
					""");
			return;
		}

		broker.advanceOnboarding(BrokerOnboardingStep.BUDGET);
		saveAndSend(broker, """
				💰 What rental budget do you handle?

				Example:
				20k-30k
				25k
				""");
	}

	private void handleBudget(Broker broker, String text) {

		Integer min;
		Integer max;

		try {
			String cleaned = text.replaceAll("\\s+", "");

			if (cleaned.contains("-")) {
				String[] parts = cleaned.split("-");
				min = parseAmount(parts[0]);
				max = parseAmount(parts[1]);
			} else {
				min = parseAmount(cleaned);
				max = min;
			}

		} catch (Exception e) {
			send(broker, "Invalid budget format. Example: 20k-30k");
			return;
		}

		brokerPreferenceService.updateBudget(broker.getId(), min, max);

		broker.advanceOnboarding(BrokerOnboardingStep.BHK);
		saveAndSend(broker, """
				🏠 Which property types do you deal in?

				Reply with:
				1BHK, 2BHK, 3BHK (comma separated)
				""");
	}

	private void handleBhk(Broker broker, String text) {

		if (!text.matches(".*\\d+bhk.*")) {
			send(broker, "Please reply like: 1BHK, 2BHK");
			return;
		}

		List<String> bhks = List.of(text.toUpperCase().split(","));

		brokerPreferenceService.updateBhks(broker.getId(), bhks);

		broker.advanceOnboarding(BrokerOnboardingStep.DONE);
		broker.activate();
		brokerRepository.save(broker);

		send(broker, """
				✅ You're all set!

				You will now start receiving tenant enquiries.
				""");
	}

	/* ---------------- HELPERS ---------------- */

	private void saveAndSend(Broker broker, String message) {
		brokerRepository.save(broker);
		send(broker, message);
	}

	private void send(Broker broker, String message) {
		whatsAppSender.sendTextMessage(broker.getPhone(), message);
	}

	private Integer parseAmount(String value) {
		value = value.toLowerCase();
		if (value.endsWith("k")) {
			return Integer.parseInt(value.replace("k", "")) * 1000;
		}
		return Integer.parseInt(value);
	}
}
