package com.risingbee.realestate.automation.service;

import org.springframework.stereotype.Service;

import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.parser.ParsedRequest;
import com.risingbee.realestate.automation.parser.SimpleParser;
import com.risingbee.realestate.automation.repo.BrokerRepository;
import com.risingbee.realestate.enums.BrokerOnboardingStep;
import com.risingbee.realestate.enums.BrokerStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerOnboardingService {

    private final BrokerRepository brokerRepository;
    private final WhatsAppSender whatsAppSender;

    public void handle(Broker broker, String text, String phone) {

        switch (broker.getOnboardingStep()) {

            case BUDGET -> handleBudget(broker, text, phone);

            case AREAS -> handleLocation(broker, text, phone);

            case BHK -> handleBhk(broker, text, phone);

            case DONE -> whatsAppSender.sendTextMessage(
                    phone,
                    "✅ Your onboarding is complete."
            );
        }
    }
    
    
    private void handleBudget(Broker broker, String text, String phone) {

        ParsedRequest parsed = SimpleParser.parse(text);

        Integer min = parsed.minBudget();
        Integer max = parsed.maxBudget();

        if (min == null && max == null) {
            whatsAppSender.sendTextMessage(
                    phone,
                    """
                    Please tell your preferred rent budget.

                    Examples:
                    • under 30k
                    • 20k to 50k
                    • minimum 25k
                    """
            );
            return;
        }

        broker.setMinBudget(min);
        broker.setMaxBudget(max);
        broker.setOnboardingStep(BrokerOnboardingStep.AREAS);

        brokerRepository.save(broker);

        whatsAppSender.sendTextMessage(
                phone,
                """
                ✅ Budget saved.

                Now tell me the locations you operate in.
                Example:
                • golf course road
                • sector 56
                • dlf phase 2
                """
        );
    }
    
    
    private void handleBhk(Broker broker, String text, String phone) {

        if (!text.contains("bhk")) {
            whatsAppSender.sendTextMessage(
                    phone,
                    "Please reply with BHK preference (e.g. 2bhk, 3bhk)."
            );
            return;
        }

        broker.setBhkPreference(text.toUpperCase());
        broker.setOnboardingStep(BrokerOnboardingStep.DONE);
        broker.setStatus(BrokerStatus.ACTIVE);

        brokerRepository.save(broker);

        whatsAppSender.sendTextMessage(
                phone,
                """
                🎉 Onboarding completed!

                You will now start receiving property enquiries.
                """
        );
    }
    
    
    private void handleLocation(Broker broker, String text, String phone) {

        if (text.length() < 3) {
            whatsAppSender.sendTextMessage(
                    phone,
                    "Please send valid locations (comma separated)."
            );
            return;
        }

        broker.setLocations(text.toLowerCase());
        broker.setOnboardingStep(BrokerOnboardingStep.BHK);

        brokerRepository.save(broker);

        whatsAppSender.sendTextMessage(
                phone,
                """
                ✅ Locations saved.

                What BHKs do you handle?
                Example:
                • 1bhk
                • 2bhk, 3bhk
                """
        );
    }

    


    
}
