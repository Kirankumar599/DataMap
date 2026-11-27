package com.datamap.engine.detector;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.DetectionResult;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detector for Indian Permanent Account Number (PAN).
 *
 * <p>Format rules:
 * - Exactly 10 characters: [A-Z]{5}[0-9]{4}[A-Z]
 * - Fourth character indicates PAN card holder type:
 *   P (Individual), C (Company), H (HUF), F (Firm), A (Association),
 *   T (Trust), B (Body of Individuals), L (Local Authority),
 *   J (Artificial Juridical Person), G (Government).
 * - Case-insensitive with safe normalization.
 * </p>
 */
public class PanDetector implements IdentifierDetector {

    // Valid 4th character PAN holder types
    private static final Set<Character> VALID_HOLDER_TYPES = Set.of(
        'P', 'C', 'H', 'F', 'A', 'T', 'B', 'L', 'J', 'G'
    );

    private static final Pattern PAN_PATTERN =
        Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    @Override
    public IdentifierType getType() {
        return IdentifierType.PAN;
    }

    @Override
    public boolean matchesFormat(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.length() != 10) {
            return false;
        }
        if (!PAN_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        char holderType = normalized.charAt(3);
        return VALID_HOLDER_TYPES.contains(holderType);
    }

    @Override
    public Double calculateChecksumRate(List<String> values) {
        // PAN does not use a public algorithmic checksum; return null
        return null;
    }

    @Override
    public DetectionResult evaluate(List<String> sampledValues) {
        if (sampledValues == null || sampledValues.isEmpty()) {
            return new DetectionResult(getType(), 0.0, null, 0, 0);
        }

        int total = sampledValues.size();
        int matched = 0;

        for (String val : sampledValues) {
            if (matchesFormat(val)) {
                matched++;
            }
        }

        double matchRate = (double) matched / total;
        return new DetectionResult(getType(), matchRate, null, matched, total);
    }
}
