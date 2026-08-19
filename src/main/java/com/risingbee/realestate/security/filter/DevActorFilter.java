package com.risingbee.realestate.security.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.risingbee.realestate.automation.actor.Actor;
import com.risingbee.realestate.automation.actor.ActorContext;
import com.risingbee.realestate.automation.actor.enums.ActorType;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

//@Component
public class DevActorFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        // ⚠️ TEMP: hardcoded actor
        Actor actor = new Actor(
                ActorType.BROKER,
                "9999999999",
                1L
        );

        ActorContext.set(actor);

        try {
            chain.doFilter(request, response);
        } finally {
            ActorContext.clear();
        }
    }
}
