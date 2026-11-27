package com.datamap.engine.detector;

import com.datamap.engine.checksum.VerhoeffChecksum;
import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.DetectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AadhaarDetectorTest {

    private AadhaarDetector detector;

    @BeforeEach
    void setUp() {
        detector = new AadhaarDetector();
    }

    @Test
    @DisplayName("Matches valid 12-digit formats with spaces and hyphens")
    void testFormatMatching() {
        assertThat(detector.getType()).isEqualTo(IdentifierType.AADHAAR);

        // First digit 2-9
        assertThat(detector.matchesFormat("234567890124")).isTrue();
        assertThat(detector.matchesFormat("2345 6789 0124")).isTrue();
        assertThat(detector.matchesFormat("2345-6789-0124")).isTrue();
        assertThat(detector.matchesFormat("9999 8888 7777")).isTrue();

        // Invalid first digit (0 or 1)
        assertThat(detector.matchesFormat("012345678901")).isFalse();
        assertThat(detector.matchesFormat("123456789012")).isFalse();

        // Invalid length
        assertThat(detector.matchesFormat("23456789012")).isFalse();
        assertThat(detector.matchesFormat("2345678901234")).isFalse();

        // Invalid chars
        assertThat(detector.matchesFormat("2345ABCD0124")).isFalse();
        assertThat(detector.matchesFormat(null)).isFalse();
        assertThat(detector.matchesFormat("")).isFalse();
    }

    @Test
    @DisplayName("Evaluates Verhoeff checksum pass rate")
    void testChecksumEvaluation() {
        String base = "45678901234";
        int validCd = VerhoeffChecksum.calculateCheckDigit(base);
        String validNum = base + validCd;
        String invalidNum = base + ((validCd + 1) % 10);

        List<String> samples = List.of(validNum, invalidNum);
        Double rate = detector.calculateChecksumRate(samples);
        assertThat(rate).isEqualTo(0.5);

        DetectionResult result = detector.evaluate(samples);
        assertThat(result.matchRate()).isEqualTo(1.0);
        assertThat(result.checksumRate()).isEqualTo(0.5);
    }
}
