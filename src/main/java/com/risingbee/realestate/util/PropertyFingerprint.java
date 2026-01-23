package com.risingbee.realestate.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.risingbee.realestate.automation.domain.Broker;
import com.risingbee.realestate.automation.domain.Property;
import com.risingbee.realestate.automation.domain.BrokerConversation;

public final class PropertyFingerprint {

    private PropertyFingerprint() {}

    /* ======================
       From Conversation
       ====================== */

    public static String fromConversation(
        Broker broker,
        BrokerConversation conv,
        String cityCode,
        String localityCode
    ) {

        String raw = String.join("|",
            broker.getId().toString(),
            safe(conv.getBhk()),
            safe(cityCode),
            safe(localityCode),
            String.valueOf(conv.getPrice())
        );

        return sha256(raw);
    }

    /* ======================
       From Property
       ====================== */

    public static String fromProperty(
        Property property
    ) {

        String raw = String.join("|",
            property.getOwnerAccountId().toString(),
            safe(property.getBhk()),
            safe(property.getCityCode()),
            safe(property.getLocalityCode()),
            String.valueOf(property.getPrice())
        );

        return sha256(raw);
    }

    /* ======================
       Helpers
       ====================== */

    private static String safe(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (Exception e) {
            throw new IllegalStateException(
                "Fingerprint generation failed",
                e
            );
        }
    }
}
