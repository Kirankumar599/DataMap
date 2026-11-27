package com.datamap.engine.checksum;

/**
 * Implementation of the Verhoeff checksum algorithm for Indian Aadhaar validation.
 *
 * <p>The Verhoeff algorithm uses the dihedral group D5 to detect all single-digit
 * errors and all adjacent transposition errors.</p>
 */
public final class VerhoeffChecksum {

    // Multiplication table for the dihedral group D5
    private static final int[][] D = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        {1, 2, 3, 4, 0, 6, 7, 8, 9, 5},
        {2, 3, 4, 0, 1, 7, 8, 9, 5, 6},
        {3, 4, 0, 1, 2, 8, 9, 5, 6, 7},
        {4, 0, 1, 2, 3, 9, 5, 6, 7, 8},
        {5, 9, 8, 7, 6, 0, 4, 3, 2, 1},
        {6, 5, 9, 8, 7, 1, 0, 4, 3, 2},
        {7, 6, 5, 9, 8, 2, 1, 0, 4, 3},
        {8, 7, 6, 5, 9, 3, 2, 1, 0, 4},
        {9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
    };

    // Permutation table P (8 permutations of digits 0-9)
    private static final int[][] P = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        {1, 5, 7, 6, 2, 8, 3, 0, 9, 4},
        {5, 8, 0, 3, 7, 9, 6, 1, 4, 2},
        {8, 9, 1, 6, 0, 4, 3, 5, 2, 7},
        {9, 4, 5, 3, 1, 2, 6, 8, 7, 0},
        {4, 2, 8, 6, 5, 7, 3, 9, 0, 1},
        {2, 7, 9, 3, 8, 0, 6, 4, 1, 5},
        {7, 0, 4, 6, 9, 1, 3, 2, 5, 8}
    };

    // Inverse table
    private static final int[] INV = {0, 4, 3, 2, 1, 5, 6, 7, 8, 9};

    private VerhoeffChecksum() {
        // Utility class
    }

    /**
     * Validates a numeric string against the Verhoeff checksum.
     *
     * @param number the numeric string to validate (may contain non-digits which are ignored)
     * @return true if valid according to Verhoeff checksum, false otherwise
     */
    public static boolean validate(String number) {
        if (number == null) {
            return false;
        }
        String clean = number.replaceAll("\\D", "");
        if (clean.isEmpty()) {
            return false;
        }

        int c = 0;
        int len = clean.length();
        for (int i = 0; i < len; i++) {
            int digit = clean.charAt(len - 1 - i) - '0';
            if (digit < 0 || digit > 9) {
                return false;
            }
            c = D[c][P[i % 8][digit]];
        }
        return c == 0;
    }

    /**
     * Calculates the Verhoeff check digit for a string of digits.
     *
     * @param numberWithoutCheckDigit number string without check digit
     * @return check digit (0-9)
     */
    public static int calculateCheckDigit(String numberWithoutCheckDigit) {
        if (numberWithoutCheckDigit == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        String clean = numberWithoutCheckDigit.replaceAll("\\D", "");
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Input must contain at least one digit");
        }

        // Test digits 0-9 to guarantee exact alignment with validate()
        for (int d = 0; d <= 9; d++) {
            if (validate(clean + d)) {
                return d;
            }
        }
        throw new IllegalStateException("Failed to calculate Verhoeff check digit");
    }
}
