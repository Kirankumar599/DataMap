package com.datamap.engine.masker;

import com.datamap.engine.enums.IdentifierType;

/**
 * Pure Java masking functions for sensitive identifiers.
 *
 * <p>Never crashes on malformed data; returns conservative masked strings.</p>
 */
public final class Masker {

    private Masker() {
        // Utility class
    }

    /**
     * Masks an identifier according to its detected type.
     */
    public static String mask(String value, IdentifierType type) {
        if (value == null) {
            return null;
        }
        if (type == null) {
            return value;
        }
        return switch (type) {
            case AADHAAR -> maskAadhaar(value);
            case PAN -> maskPan(value);
            case PHONE -> maskPhone(value);
            case EMAIL -> maskEmail(value);
        };
    }

    /**
     * Masks Aadhaar number to format: XXXX-XXXX-0124 (last 4 digits preserved).
     */
    public static String maskAadhaar(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "XXXX-XXXX-XXXX";
        }
        String last4 = digits.substring(digits.length() - 4);
        return "XXXX-XXXX-" + last4;
    }

    /**
     * Masks PAN number to format: XXXXXX234F (first 6 characters masked, last 4 preserved).
     */
    public static String maskPan(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 4) {
            return "XXXXXXXXXX";
        }
        String last4 = trimmed.substring(trimmed.length() - 4).toUpperCase();
        return "XXXXXX" + last4;
    }

    /**
     * Masks Indian mobile number to format: XXXXXX3210 (last 4 digits preserved).
     */
    public static String maskPhone(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "XXXXXX0000";
        }
        String last4 = digits.substring(digits.length() - 4);
        return "XXXXXX" + last4;
    }

    /**
     * Masks email address to format: r***@gmail.com (first char of local part + *** + @domain).
     */
    public static String maskEmail(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0 || atIndex == trimmed.length() - 1) {
            return "***@***";
        }
        String local = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex); // includes '@'
        char firstChar = local.charAt(0);
        return firstChar + "***" + domain;
    }

    /**
     * Conservative fallback masking for manually selected unflagged columns.
     * Preserves no raw characters for short values and only up to the last 2 characters for longer values.
     */
    public static String maskGeneric(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.length() <= 4) {
            return "XXXX";
        }
        int visibleSuffix = trimmed.length() >= 8 ? 4 : 2;
        return "X".repeat(trimmed.length() - visibleSuffix) + trimmed.substring(trimmed.length() - visibleSuffix);
    }
}
