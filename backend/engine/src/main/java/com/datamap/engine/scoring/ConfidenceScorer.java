package com.datamap.engine.scoring;

import com.datamap.engine.enums.Confidence;
import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.DetectionResult;
import com.datamap.engine.model.HeaderMatchResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates detection signals and produces confidence scores and plain-English reasons.
 *
 * <p>Signals considered:
 * - Header keyword match (exact / fuzzy)
 * - Value match rate (percentage of sampled values matching format)
 * - Aadhaar Verhoeff checksum pass rate
 * </p>
 */
public class ConfidenceScorer {

    public static final double STRONG_MATCH_THRESHOLD = 0.80;
    public static final double WEAK_MATCH_THRESHOLD = 0.30;
    public static final double CHECKSUM_STRONG_THRESHOLD = 0.80;
    public static final double CHECKSUM_FAIL_THRESHOLD = 0.50;

    public record ScoredFinding(
        Confidence confidence,
        List<String> reasons
    ) {}

    /**
     * Determines whether signals warrant flagging a column and computes confidence and reasons.
     *
     * @return ScoredFinding if column meets threshold, or null if below threshold
     */
    public ScoredFinding score(
        HeaderMatchResult headerResult,
        DetectionResult detectionResult
    ) {
        double matchRate = detectionResult.matchRate();
        Double checksumRate = detectionResult.checksumRate();
        boolean headerMatch = headerResult.isMatch();
        IdentifierType type = detectionResult.type();

        List<String> reasons = new ArrayList<>();

        // Generate reasons for header signal
        if (headerMatch) {
            if (headerResult.isFuzzy()) {
                int pct = (int) Math.round(headerResult.similarity() * 100);
                reasons.add(String.format("Header fuzzy matches '%s' (%d%% similarity)",
                    headerResult.matchedKeyword(), pct));
            } else {
                reasons.add(String.format("Header matches '%s'", headerResult.matchedKeyword()));
            }
        }

        // Generate reasons for value match signal
        int matchPct = (int) Math.round(matchRate * 100);
        if (matchRate > 0) {
            String formatName = getFormatDescription(type);
            reasons.add(String.format("%d%% of sampled values match %s", matchPct, formatName));
        }

        // Generate reasons for checksum signal (Aadhaar only)
        if (type == IdentifierType.AADHAAR && checksumRate != null && detectionResult.matchedCount() > 0) {
            int csPct = (int) Math.round(checksumRate * 100);
            if (checksumRate >= CHECKSUM_STRONG_THRESHOLD) {
                reasons.add(String.format("%d%% of Aadhaar-like values pass the Verhoeff checksum", csPct));
            } else if (checksumRate < CHECKSUM_FAIL_THRESHOLD) {
                reasons.add(String.format("Several Aadhaar-like values failed checksum validation (%d%% pass rate)", csPct));
            } else {
                reasons.add(String.format("%d%% of Aadhaar-like values pass the Verhoeff checksum", csPct));
            }
        }

        // Threshold checks:
        // If neither header matched nor value match rate >= 30%, do not flag
        if (!headerMatch && matchRate < WEAK_MATCH_THRESHOLD) {
            return null;
        }

        // If header matched but 0% values match format and total sampled > 0, do not flag (empty or completely mismatched data)
        if (headerMatch && matchRate == 0.0 && detectionResult.totalSampled() > 0) {
            return null;
        }

        Confidence confidence;

        if (type == IdentifierType.AADHAAR) {
            confidence = scoreAadhaar(headerMatch, matchRate, checksumRate);
        } else {
            confidence = scoreStandard(headerMatch, matchRate);
        }

        if (confidence == null) {
            return null;
        }

        return new ScoredFinding(confidence, reasons);
    }

    private Confidence scoreStandard(boolean headerMatch, double matchRate) {
        if (headerMatch && matchRate >= STRONG_MATCH_THRESHOLD) {
            return Confidence.HIGH;
        }
        if (matchRate >= STRONG_MATCH_THRESHOLD) {
            return Confidence.MEDIUM;
        }
        if (headerMatch && matchRate >= WEAK_MATCH_THRESHOLD) {
            return Confidence.MEDIUM;
        }
        if (headerMatch || matchRate >= WEAK_MATCH_THRESHOLD) {
            return Confidence.LOW;
        }
        return null;
    }

    private Confidence scoreAadhaar(boolean headerMatch, double matchRate, Double checksumRate) {
        double cs = (checksumRate != null) ? checksumRate : 0.0;

        // If checksum pass rate is very low (e.g. < 20%) despite matching 12-digit format,
        // it strongly indicates these are arbitrary numbers (like account IDs), not Aadhaar
        if (matchRate >= STRONG_MATCH_THRESHOLD && cs < 0.20 && !headerMatch) {
            return null; // Not an Aadhaar number
        }

        if (headerMatch && matchRate >= STRONG_MATCH_THRESHOLD && cs >= CHECKSUM_STRONG_THRESHOLD) {
            return Confidence.HIGH;
        }
        if (matchRate >= STRONG_MATCH_THRESHOLD && cs >= CHECKSUM_STRONG_THRESHOLD) {
            // High value + high checksum even without header
            return Confidence.MEDIUM;
        }
        if (headerMatch && matchRate >= STRONG_MATCH_THRESHOLD && cs >= CHECKSUM_FAIL_THRESHOLD) {
            return Confidence.MEDIUM;
        }
        if (headerMatch && matchRate >= WEAK_MATCH_THRESHOLD) {
            return Confidence.MEDIUM;
        }
        if (headerMatch || (matchRate >= WEAK_MATCH_THRESHOLD && cs >= CHECKSUM_FAIL_THRESHOLD)) {
            return Confidence.LOW;
        }
        return null;
    }

    private String getFormatDescription(IdentifierType type) {
        return switch (type) {
            case AADHAAR -> "the 12-digit format";
            case PAN -> "the 10-character PAN format";
            case PHONE -> "the Indian mobile format";
            case EMAIL -> "the email address format";
        };
    }
}
