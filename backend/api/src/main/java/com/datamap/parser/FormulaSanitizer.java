package com.datamap.parser;

/**
 * Neutralizes spreadsheet formula-injection payloads in exported CSV and XLSX cells.
 *
 * <p>Cells starting with {@code =}, {@code +}, {@code -}, {@code @}, tab, or carriage return
 * are prefixed with a single quote ({@code '}) so spreadsheet applications treat them as literal text.</p>
 */
public final class FormulaSanitizer {

    private FormulaSanitizer() {
        // Utility class
    }

    /**
     * Neutralizes potentially dangerous leading formula characters in an exported cell value.
     *
     * @param value cell text to sanitize
     * @return sanitized string (empty string if null)
     */
    public static String sanitizeCell(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }
}
