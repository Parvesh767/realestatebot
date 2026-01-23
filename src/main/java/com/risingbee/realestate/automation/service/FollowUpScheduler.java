package com.risingbee.realestate.automation.service;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FollowUpScheduler {

   
    // run every hour
    @Scheduled(fixedRateString = "PT1H")
    public void runFollowUps() {
        // find leads that need follow-up (simple rule: createdAt older than X and not contacted)
        // send follow-up message via whatsAppSender
    }
}
