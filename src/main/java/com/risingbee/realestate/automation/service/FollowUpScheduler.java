package com.risingbee.realestate.automation.service;


import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.risingbee.realestate.automation.repo.LeadRepository;

@Component
@RequiredArgsConstructor
public class FollowUpScheduler {

    private final LeadRepository leadRepository;
    private final WhatsAppSender whatsAppSender;

    // run every hour
    @Scheduled(fixedRateString = "PT1H")
    public void runFollowUps() {
        // find leads that need follow-up (simple rule: createdAt older than X and not contacted)
        // send follow-up message via whatsAppSender
    }
}
