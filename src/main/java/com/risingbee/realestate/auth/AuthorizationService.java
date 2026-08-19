package com.risingbee.realestate.auth;
import com.risingbee.realestate.automation.actor.enums.*;
import com.risingbee.realestate.automation.actor.*;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    public void require(Actor actor, Capability capability) {
        if (actor == null) {
            throw new AccessDeniedException("Unauthenticated actor");
        }
        

        if (actor.role() == null) {
            throw new AccessDeniedException("Role not assigned");
        }

        if (!ActorCapabilities.allows(actor.role(), capability)) {
            throw new AccessDeniedException("Access denied");
        }

        if (!ActorCapabilities.allows(actor, capability)) {
            throw new AccessDeniedException(
                "Actor " + actor + " lacks capability: " + capability
            );
        }
    }
}
