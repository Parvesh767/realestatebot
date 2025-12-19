package com.risingbee.realestate.automation.service;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.repo.BrokerRepository;
import com.risingbee.realestate.enums.BrokerOnboardingStep;
import com.risingbee.realestate.enums.BrokerStatus;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerOnboardingServiceImpl implements com.risingbee.realestate.automation.interfaces.BrokerOnboardingService {

    private final BrokerRepository brokerRepository;
    private final WhatsAppSender whatsAppSender;

    @Override
    @Transactional
    public void handle(Broker broker, String message) {

        String text = message.trim().toLowerCase();

        switch (broker.getOnboardingStep()) {

            case START -> askLocation(broker);

            case AREAS -> handleLocation(broker, text);

            case BUDGET -> handleBudget(broker, text);

            case BHK -> handleBhk(broker, text);

            case DONE -> {
                // safety net
                broker.setStatus(BrokerStatus.ACTIVE);
                brokerRepository.save(broker);
            }
        }
    }

    /* ---------------- STEP HANDLERS ---------------- */

    private void askLocation(Broker broker) {
        whatsAppSender.sendTextMessage(
                broker.getPhone(),
                """
                👋 Welcome!

                Which areas do you deal in?
                (Example: Gurgaon, Sector 56, Golf Course Road)
                """
        );
        broker.setOnboardingStep(BrokerOnboardingStep.AREAS);
        brokerRepository.save(broker);
    }

    private void handleLocation(Broker broker, String text) {

        if (text.length() < 3) {
            whatsAppSender.sendTextMessage(
                    broker.getPhone(),
                    "Please enter at least one valid location."
            );
            return;
        }

        broker.setLocations(text);
        broker.setOnboardingStep(BrokerOnboardingStep.BUDGET);
        brokerRepository.save(broker);

        whatsAppSender.sendTextMessage(
                broker.getPhone(),
                """
                💰 What rental budget do you handle?

                Example:
                20k-30k
                25k
                """
        );
    }

    private void handleBudget(Broker broker, String text) {

        Integer min = null;
        Integer max = null;

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
            whatsAppSender.sendTextMessage(
                    broker.getPhone(),
                    "Invalid budget format. Example: 20k-30k"
            );
            return;
        }

        broker.setMinBudget(min);
        broker.setMaxBudget(max);
        broker.setOnboardingStep(BrokerOnboardingStep.BHK);
        brokerRepository.save(broker);

        whatsAppSender.sendTextMessage(
                broker.getPhone(),
                """
                🏠 Which property types do you deal in?

                Reply with:
                1BHK, 2BHK, 3BHK (comma separated)
                """
        );
    }

    private void handleBhk(Broker broker, String text) {

        if (!text.matches(".*\\d+bhk.*")) {
            whatsAppSender.sendTextMessage(
                    broker.getPhone(),
                    "Please reply like: 1BHK, 2BHK"
            );
            return;
        }

        broker.setBhkPreference(text.toUpperCase());
        broker.setOnboardingStep(BrokerOnboardingStep.DONE);
        broker.setStatus(BrokerStatus.ACTIVE);

        brokerRepository.save(broker);

        whatsAppSender.sendTextMessage(
                broker.getPhone(),
                """
                ✅ You're all set!

                You will now start receiving tenant enquiries.
                """
        );
    }

    /* ---------------- UTIL ---------------- */

    private Integer parseAmount(String value) {
        value = value.toLowerCase();

        if (value.endsWith("k")) {
            return Integer.parseInt(value.replace("k", "")) * 1000;
        }
        return Integer.parseInt(value);
    }
}

