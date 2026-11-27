package com.datamap.engine.detector;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.DetectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailDetectorTest {

    private EmailDetector detector;

    @BeforeEach
    void setUp() {
        detector = new EmailDetector();
    }

    @Test
    @DisplayName("Matches valid common and subdomained email addresses")
    void testValidEmails() {
        assertThat(detector.getType()).isEqualTo(IdentifierType.EMAIL);

        assertThat(detector.matchesFormat("ravi.kumar@gmail.com")).isTrue();
        assertThat(detector.matchesFormat("user_name123@domain.co.in")).isTrue();
        assertThat(detector.matchesFormat("test+tag@sub.domain.org")).isTrue();
        assertThat(detector.matchesFormat("first.last@company.io")).isTrue();
    }

    @Test
    @DisplayName("Rejects malformed emails, consecutive dots, and edge cases")
    void testInvalidEmails() {
        assertThat(detector.matchesFormat("plainaddress")).isFalse();
        assertThat(detector.matchesFormat("@missinglocal.com")).isFalse();
        assertThat(detector.matchesFormat("missingdomain@")).isFalse();
        assertThat(detector.matchesFormat("missing.at.sign.com")).isFalse();
        assertThat(detector.matchesFormat("user..name@gmail.com")).isFalse();
        assertThat(detector.matchesFormat(".leadingdot@gmail.com")).isFalse();
        assertThat(detector.matchesFormat("trailingdot.@gmail.com")).isFalse();
        assertThat(detector.matchesFormat(null)).isFalse();
        assertThat(detector.matchesFormat("")).isFalse();
        assertThat(detector.matchesFormat("   ")).isFalse();
    }

    @Test
    @DisplayName("Evaluates email match rate correctly")
    void testEvaluation() {
        List<String> samples = List.of("user@test.com", "admin@domain.org", "invalid-email");
        DetectionResult result = detector.evaluate(samples);

        assertThat(result.matchRate()).isEqualTo(2.0 / 3.0);
        assertThat(result.checksumRate()).isNull();
    }
}
