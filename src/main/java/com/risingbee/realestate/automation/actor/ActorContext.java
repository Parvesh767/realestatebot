package com.risingbee.realestate.automation.actor;

import com.risingbee.realestate.automation.actor.enums.Capability;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ActorContext {

    private static final ThreadLocal<Actor> CURRENT = new ThreadLocal<>();  
    

    private ActorContext() {}

    /**
     * Set the current actor for this thread.
     * This should be called ONCE per request.
     */
    public static void set(Actor actor) {

        if (actor == null) {
            log.warn("Attempted to SET null Actor into ActorContext. Ignored.");
            return;
        }

        Actor existing = CURRENT.get();
        if (existing != null) {
            log.warn(
                "ActorContext already set. Overwriting existing actor {} with {}",
                existing, actor
            );
        }

        CURRENT.set(actor);
        log.debug("ActorContext SET → {}", actor);
    }

    /**
     * Get the current actor, or null if none set.
     * Read-only phase: no exceptions yet.
     */
    public static Actor get() {
        Actor actor = CURRENT.get();
        if (actor == null) {
            log.trace("ActorContext GET → null (no actor set)");
        }
        log.info("ActorContext GET → {} "  , actor);
        return actor;
    }

    /**
     * Clear actor context at request end.
     */
    public static void clear() {
        Actor actor = CURRENT.get();
        if (actor != null) {
            log.debug("ActorContext CLEAR → {}", actor);
        } else {
            log.trace("ActorContext CLEAR → already empty");
        }
        CURRENT.remove();
    }
    
    
    public static boolean hasCapability(Capability capability) {
        Actor actor = get();
        return actor != null &&
               ActorCapabilities.allows(actor.role(), capability);
    }

    public static void requireCapability(Capability capability) {
        Actor actor = get();
        if (actor == null) {
            throw new IllegalStateException("No actor in context");
        }
        if (!ActorCapabilities.allows(actor.role(), capability)) {
            throw new IllegalStateException(
                "Actor " + actor + " lacks capability " + capability
            );
        }
    }
}
