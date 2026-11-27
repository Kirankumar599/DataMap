package com.datamap.engine.detector;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.DetectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneDetectorTest {

    private PhoneDetector detector;

    @BeforeEach
    void setUp() {
        detector = new PhoneDetector();
    }

    @Test
    @DisplayName("Matches 10-digit Indian mobile numbers starting with 6, 7, 8, 9")
    void testIndianMobileNumbers() {
        assertThat(detector.getType()).isEqualTo(IdentifierType.PHONE);

        assertThat(detector.matchesFormat("9876543210")).isTrue();
        assertThat(detector.matchesFormat("8123456789")).isTrue();
        assertThat(detector.matchesFormat("7000112233")).isTrue();
        assertThat(detector.matchesFormat("6300112233")).isTrue();

        // Must not start with 1, 2, 3, 4, 5
        assertThat(detector.matchesFormat("5123456789")).isFalse();
        assertThat(detector.matchesFormat("1234567890")).isFalse();
    }

    @Test
    @DisplayName("Supports prefixes: +91, 91, 0 with spaces and hyphens")
    void testPrefixesAndFormatting() {
        assertThat(detector.matchesFormat("+91 9876543210")).isTrue();
        assertThat(detector.matchesFormat("+91-98765-43210")).isTrue();
        assertThat(detector.matchesFormat("919876543210")).isTrue();
        assertThat(detector.matchesFormat("09876543210")).isTrue();
        assertThat(detector.matchesFormat("(0) 98765 43210")).isTrue();
    }

    @Test
    @DisplayName("Rejects invalid phone numbers and nulls")
    void testInvalidPhone() {
        assertThat(detector.matchesFormat("987654321")).isFalse(); // 9 digits
        assertThat(detector.matchesFormat("987654321000")).isFalse(); // too long
        assertThat(detector.matchesFormat("98765ABCDE")).isFalse();
        assertThat(detector.matchesFormat(null)).isFalse();
        assertThat(detector.matchesFormat("")).isFalse();
    }

    @Test
    @DisplayName("Evaluates match rate correctly")
    void testEvaluation() {
        List<String> samples = List.of("9876543210", "+91 8888877777", "not-a-phone");
        DetectionResult result = detector.evaluate(samples);

        assertThat(result.matchRate()).isEqualTo(2.0 / 3.0);
        assertThat(result.checksumRate()).isNull();
    }
}
