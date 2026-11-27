package com.datamap.engine.masker;

import com.datamap.engine.enums.IdentifierType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskerTest {

    @Test
    @DisplayName("Masks Aadhaar preserving only last 4 digits in format XXXX-XXXX-0124")
    void testAadhaarMasking() {
        assertThat(Masker.maskAadhaar("234567890124")).isEqualTo("XXXX-XXXX-0124");
        assertThat(Masker.maskAadhaar("2345 6789 0124")).isEqualTo("XXXX-XXXX-0124");
        assertThat(Masker.maskAadhaar("2345-6789-0124")).isEqualTo("XXXX-XXXX-0124");
    }

    @Test
    @DisplayName("Masks PAN preserving last 4 characters in format XXXXXX234F")
    void testPanMasking() {
        assertThat(Masker.maskPan("ABCDE1234F")).isEqualTo("XXXXXX234F");
        assertThat(Masker.maskPan("abcpk1234f")).isEqualTo("XXXXXX234F");
    }

    @Test
    @DisplayName("Masks Indian mobile preserving last 4 digits in format XXXXXX3210")
    void testPhoneMasking() {
        assertThat(Masker.maskPhone("9876543210")).isEqualTo("XXXXXX3210");
        assertThat(Masker.maskPhone("+91 98765 43210")).isEqualTo("XXXXXX3210");
        assertThat(Masker.maskPhone("09876543210")).isEqualTo("XXXXXX3210");
    }

    @Test
    @DisplayName("Masks email to format r***@gmail.com")
    void testEmailMasking() {
        assertThat(Masker.maskEmail("ravi.kumar@gmail.com")).isEqualTo("r***@gmail.com");
        assertThat(Masker.maskEmail("a@domain.com")).isEqualTo("a***@domain.com");
    }

    @Test
    @DisplayName("Handles null and empty strings safely")
    void testNullAndEmpty() {
        assertThat(Masker.mask(null, IdentifierType.AADHAAR)).isNull();
        assertThat(Masker.maskAadhaar(null)).isNull();
        assertThat(Masker.maskPan(null)).isNull();
        assertThat(Masker.maskPhone(null)).isNull();
        assertThat(Masker.maskEmail(null)).isNull();
    }

    @Test
    @DisplayName("Handles malformed strings conservatively without throwing exceptions")
    void testMalformed() {
        assertThat(Masker.maskAadhaar("12")).isEqualTo("XXXX-XXXX-XXXX");
        assertThat(Masker.maskPan("12")).isEqualTo("XXXXXXXXXX");
        assertThat(Masker.maskPhone("12")).isEqualTo("XXXXXX0000");
        assertThat(Masker.maskEmail("notanemail")).isEqualTo("***@***");
    }
}
