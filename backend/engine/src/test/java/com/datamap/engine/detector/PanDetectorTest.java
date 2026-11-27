package com.datamap.engine.detector;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.DetectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PanDetectorTest {

    private PanDetector detector;

    @BeforeEach
    void setUp() {
        detector = new PanDetector();
    }

    @Test
    @DisplayName("Matches valid PAN format and valid holder types")
    void testValidPan() {
        assertThat(detector.getType()).isEqualTo(IdentifierType.PAN);

        // P - Individual, C - Company, H - HUF, F - Firm, A - Association, T - Trust
        assertThat(detector.matchesFormat("ABCDE1234F")).isFalse(); // 4th char is D (invalid)
        assertThat(detector.matchesFormat("ABCPK1234F")).isTrue();  // 4th char P (valid)
        assertThat(detector.matchesFormat("XYZCA5678G")).isTrue();  // 4th char C (valid)
        assertThat(detector.matchesFormat("MNPHB9012K")).isTrue();  // 4th char H (valid)
        assertThat(detector.matchesFormat("QWERT1234Y")).isFalse(); // 4th char is 'R' (invalid holder type)
        assertThat(detector.matchesFormat("abcpf1234k")).isTrue();  // 4th char P (valid, case insensitive)
    }

    @Test
    @DisplayName("Rejects invalid 4th character PAN holder types")
    void testInvalidHolderType() {
        // 'Z' is not a valid PAN holder type
        assertThat(detector.matchesFormat("ABCZE1234F")).isFalse();
        // 'X' is not valid
        assertThat(detector.matchesFormat("ABCXE1234F")).isFalse();
    }

    @Test
    @DisplayName("Rejects invalid length, pattern and nulls")
    void testInvalidPan() {
        assertThat(detector.matchesFormat("ABCDE1234")).isFalse();
        assertThat(detector.matchesFormat("ABCDE12345F")).isFalse();
        assertThat(detector.matchesFormat("12345ABCDE")).isFalse();
        assertThat(detector.matchesFormat(null)).isFalse();
        assertThat(detector.matchesFormat("")).isFalse();
    }

    @Test
    @DisplayName("Calculates match rate correctly and returns null checksum rate")
    void testEvaluation() {
        List<String> samples = List.of("ABCPK1234F", "XYZCA5678G", "invalid-value");
        DetectionResult result = detector.evaluate(samples);

        assertThat(result.matchRate()).isEqualTo(2.0 / 3.0);
        assertThat(result.checksumRate()).isNull();
    }
}
