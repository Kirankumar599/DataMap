package com.datamap.engine;

import com.datamap.engine.detector.AadhaarDetector;
import com.datamap.engine.detector.EmailDetector;
import com.datamap.engine.detector.IdentifierDetector;
import com.datamap.engine.detector.PanDetector;
import com.datamap.engine.detector.PhoneDetector;
import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.header.HeaderMatcher;
import com.datamap.engine.masker.Masker;
import com.datamap.engine.model.ColumnFinding;
import com.datamap.engine.model.DetectionResult;
import com.datamap.engine.model.HeaderMatchResult;
import com.datamap.engine.scoring.ConfidenceScorer;
import com.datamap.engine.scoring.Sampler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of ColumnScanner orchestrating detectors, header matcher,
 * sampler, and confidence scorer.
 */
public class DefaultColumnScanner implements ColumnScanner {

    private final List<IdentifierDetector> detectors;
    private final HeaderMatcher headerMatcher;
    private final Sampler sampler;
    private final ConfidenceScorer scorer;

    public DefaultColumnScanner() {
        this.detectors = List.of(
            new AadhaarDetector(),
            new PanDetector(),
            new PhoneDetector(),
            new EmailDetector()
        );
        this.headerMatcher = new HeaderMatcher();
        this.sampler = new Sampler();
        this.scorer = new ConfidenceScorer();
    }

    public DefaultColumnScanner(
        List<IdentifierDetector> detectors,
        HeaderMatcher headerMatcher,
        Sampler sampler,
        ConfidenceScorer scorer
    ) {
        this.detectors = detectors;
        this.headerMatcher = headerMatcher;
        this.sampler = sampler;
        this.scorer = scorer;
    }

    @Override
    public List<ColumnFinding> scan(List<String> headers, List<List<String>> rows) {
        if (headers == null || headers.isEmpty() || rows == null) {
            return Collections.emptyList();
        }

        int columnCount = headers.size();
        List<ColumnFinding> findings = new ArrayList<>();

        for (int colIdx = 0; colIdx < columnCount; colIdx++) {
            String headerName = headers.get(colIdx);

            // Extract all cell values for this column across all rows
            List<String> columnValues = new ArrayList<>(rows.size());
            for (List<String> row : rows) {
                if (row != null && colIdx < row.size()) {
                    columnValues.add(row.get(colIdx));
                } else {
                    columnValues.add(null);
                }
            }

            // Sample non-empty values
            List<String> samples = sampler.sample(columnValues);

            // Evaluate against each detector and pick the best match
            CandidateFinding bestCandidate = null;

            for (IdentifierDetector detector : detectors) {
                IdentifierType type = detector.getType();
                HeaderMatchResult headerResult = headerMatcher.match(headerName, type);
                DetectionResult detectionResult = detector.evaluate(samples);

                ConfidenceScorer.ScoredFinding scored = scorer.score(headerResult, detectionResult);
                if (scored != null) {
                    CandidateFinding candidate = new CandidateFinding(
                        type,
                        scored.confidence(),
                        scored.reasons(),
                        detectionResult.matchRate(),
                        detectionResult.checksumRate(),
                        detector
                    );

                    if (bestCandidate == null || isHigherPriority(candidate, bestCandidate)) {
                        bestCandidate = candidate;
                    }
                }
            }

            if (bestCandidate != null) {
                // Collect 2 to 5 masked sample values that actually matched the format
                List<String> maskedSamples = collectMaskedSamples(samples, bestCandidate.detector(), bestCandidate.type());

                findings.add(new ColumnFinding(
                    colIdx,
                    headerName,
                    bestCandidate.type(),
                    bestCandidate.confidence(),
                    bestCandidate.reasons(),
                    bestCandidate.matchRate(),
                    bestCandidate.checksumRate(),
                    maskedSamples
                ));
            }
        }

        return findings;
    }

    private List<String> collectMaskedSamples(
        List<String> samples,
        IdentifierDetector detector,
        IdentifierType type
    ) {
        List<String> masked = new ArrayList<>();
        for (String sample : samples) {
            if (sample != null && detector.matchesFormat(sample)) {
                String maskedValue = Masker.mask(sample, type);
                if (!masked.contains(maskedValue)) {
                    masked.add(maskedValue);
                }
                if (masked.size() >= 5) {
                    break;
                }
            }
        }
        // If no matching format values found, mask whatever samples exist (up to 2)
        if (masked.isEmpty()) {
            for (String sample : samples) {
                if (sample != null && !sample.isBlank()) {
                    masked.add(Masker.mask(sample, type));
                    if (masked.size() >= 2) {
                        break;
                    }
                }
            }
        }
        return masked;
    }

    private boolean isHigherPriority(CandidateFinding candidate, CandidateFinding currentBest) {
        int candidateRank = confidenceRank(candidate.confidence());
        int currentRank = confidenceRank(currentBest.confidence());
        if (candidateRank != currentRank) {
            return candidateRank > currentRank;
        }
        if (Double.compare(candidate.matchRate(), currentBest.matchRate()) != 0) {
            return candidate.matchRate() > currentBest.matchRate();
        }
        // Tie-breaker: prefer PHONE over weak-checksum AADHAAR (e.g., 91-prefixed 12-digit numbers)
        if (currentBest.type() == IdentifierType.AADHAAR && candidate.type() == IdentifierType.PHONE) {
            double cs = currentBest.checksumRate() != null ? currentBest.checksumRate() : 0.0;
            if (cs < ConfidenceScorer.CHECKSUM_STRONG_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private int confidenceRank(com.datamap.engine.enums.Confidence confidence) {
        return switch (confidence) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private record CandidateFinding(
        IdentifierType type,
        com.datamap.engine.enums.Confidence confidence,
        List<String> reasons,
        double matchRate,
        Double checksumRate,
        IdentifierDetector detector
    ) {}
}
