package com.risingbee.realestate.automation.util;

public final class PhoneUtils {

    private PhoneUtils() {
        // Utility class
    }

    /**
     * Normalizes a phone number for the WhatsApp Business API.
     * - Strips all non-numeric characters (+, -, spaces, brackets).
     * - Removes leading '0' if present.
     * - Automatically prefixes 10-digit Indian numbers with country code '91'.
     *
     * @param phone Raw phone number string input
     * @return Clean digits formatted with international country code without '+'
     */
    public static String formatWhatsAppNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }

        // 1. Strip all non-digit characters
        String digits = phone.replaceAll("[^0-9]", "");

        if (digits.isEmpty()) {
            return "";
        }

        // 2. Remove leading single '0' (e.g., "08847084526" -> "8847084526")
        if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }

        // 3. Auto-prefix 10-digit standard Indian mobile numbers with '91'
        if (digits.length() == 10) {
            return "91" + digits;
        }

        return digits;
    }
}