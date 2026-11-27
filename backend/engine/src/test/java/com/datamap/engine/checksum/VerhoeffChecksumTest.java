package com.datamap.engine.checksum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerhoeffChecksumTest {

    @Test
    @DisplayName("Validates synthetically generated Verhoeff numbers")
    void testValidVerhoeff() {
        // Synthetically generate check digits for prefixes
        String base1 = "28374619283";
        int cd1 = VerhoeffChecksum.calculateCheckDigit(base1);
        String validNumber1 = base1 + cd1;

        String base2 = "98765432109";
        int cd2 = VerhoeffChecksum.calculateCheckDigit(base2);
        String validNumber2 = base2 + cd2;

        assertThat(VerhoeffChecksum.validate(validNumber1)).isTrue();
        assertThat(VerhoeffChecksum.validate(validNumber2)).isTrue();
    }

    @Test
    @DisplayName("Detects altered check digit")
    void testAlteredChecksum() {
        String base = "54321678901";
        int cd = VerhoeffChecksum.calculateCheckDigit(base);
        String valid = base + cd;

        // Alter the check digit
        int alteredCd = (cd + 1) % 10;
        String invalid = base + alteredCd;

        assertThat(VerhoeffChecksum.validate(valid)).isTrue();
        assertThat(VerhoeffChecksum.validate(invalid)).isFalse();
    }

    @Test
    @DisplayName("Detects adjacent digit transposition")
    void testAdjacentTransposition() {
        String base = "39281746502";
        int cd = VerhoeffChecksum.calculateCheckDigit(base);
        String valid = base + cd;

        // Swap two adjacent digits in the middle
        char[] chars = valid.toCharArray();
        char temp = chars[2];
        chars[2] = chars[3];
        chars[3] = temp;
        String transposed = new String(chars);

        if (!transposed.equals(valid)) {
            assertThat(VerhoeffChecksum.validate(transposed)).isFalse();
        }
    }

    @Test
    @DisplayName("Handles null, empty and invalid non-digits safely")
    void testEdgeCases() {
        assertThat(VerhoeffChecksum.validate(null)).isFalse();
        assertThat(VerhoeffChecksum.validate("")).isFalse();
        assertThat(VerhoeffChecksum.validate("   ")).isFalse();
        assertThat(VerhoeffChecksum.validate("ABCDEF")).isFalse();
    }
}
