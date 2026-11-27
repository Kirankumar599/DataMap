package com.datamap.engine.scoring;

import com.datamap.engine.enums.Confidence;
import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.DetectionResult;
import com.datamap.engine.model.HeaderMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfidenceScorerTest {

    private ConfidenceScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new ConfidenceScorer();
    }

    @Test
    @DisplayName("High confidence when header matches and value match rate is high")
    void testHighConfidence() {
        HeaderMatchResult header = new HeaderMatchResult(IdentifierType.PAN, true, false, "pan", 1.0);
        DetectionResult detection = new DetectionResult(IdentifierType.PAN, 0.95, null, 95, 100);

        ConfidenceScorer.ScoredFinding scored = scorer.score(header, detection);

        assertThat(scored).isNotNull();
        assertThat(scored.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(scored.reasons()).anyMatch(r -> r.contains("Header matches 'pan'"));
        assertThat(scored.reasons()).anyMatch(r -> r.contains("95% of sampled values match"));
    }

    @Test
    @DisplayName("Medium confidence when value match is high without header match")
    void testMediumConfidenceNoHeader() {
        HeaderMatchResult header = HeaderMatchResult.noMatch(IdentifierType.PHONE);
        DetectionResult detection = new DetectionResult(IdentifierType.PHONE, 0.90, null, 90, 100);

        ConfidenceScorer.ScoredFinding scored = scorer.score(header, detection);

        assertThat(scored).isNotNull();
        assertThat(scored.confidence()).isEqualTo(Confidence.MEDIUM);
    }

    @Test
    @DisplayName("Aadhaar high confidence requires strong checksum rate")
    void testAadhaarChecksumRequirement() {
        HeaderMatchResult header = new HeaderMatchResult(IdentifierType.AADHAAR, true, false, "aadhaar", 1.0);
        // High format match but poor checksum rate
        DetectionResult failedChecksum = new DetectionResult(IdentifierType.AADHAAR, 0.95, 0.10, 95, 100);

        ConfidenceScorer.ScoredFinding scored = scorer.score(header, failedChecksum);
        assertThat(scored).isNotNull();
        assertThat(scored.confidence()).isNotEqualTo(Confidence.HIGH);

        // High format match AND high checksum rate
        DetectionResult passedChecksum = new DetectionResult(IdentifierType.AADHAAR, 0.95, 0.95, 95, 100);
        ConfidenceScorer.ScoredFinding scoredPassed = scorer.score(header, passedChecksum);
        assertThat(scoredPassed).isNotNull();
        assertThat(scoredPassed.confidence()).isEqualTo(Confidence.HIGH);
    }

    @Test
    @DisplayName("Returns null when below minimum evidence threshold")
    void testBelowThreshold() {
        HeaderMatchResult header = HeaderMatchResult.noMatch(IdentifierType.EMAIL);
        DetectionResult detection = new DetectionResult(IdentifierType.EMAIL, 0.05, null, 5, 100);

        ConfidenceScorer.ScoredFinding scored = scorer.score(header, detection);
        assertThat(scored).isNull();
    }
}
