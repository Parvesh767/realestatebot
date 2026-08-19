package com.risingbee.realestate.automation.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.risingbee.realestate.auth.dto.BrokerResponse;
import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.service.BrokerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/broker")
@RequiredArgsConstructor
public class BrokerController {

    private final BrokerService brokerService;

    @GetMapping("/me")
    public BrokerResponse me() {
        Actor actor = ActorContext.get();
        return brokerService.getMyProfile(actor);
    }
}