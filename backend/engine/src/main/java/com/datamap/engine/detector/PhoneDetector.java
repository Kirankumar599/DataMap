package com.datamap.engine.detector;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.DetectionResult;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detector for Indian mobile numbers.
 *
 * <p>Format rules:
 * - 10-digit mobile number
 * - First digit must be 6, 7, 8, or 9
 * - Supports prefixes: +91, 91, and 0
 * - Supports spaces and hyphens
 * - Normalizes before validating
 * </p>
 */
public class PhoneDetector implements IdentifierDetector {

    private static final Pattern TEN_DIGIT_INDIAN_MOBILE =
        Pattern.compile("^[6-9]\\d{9}$");

    @Override
    public IdentifierType getType() {
        return IdentifierType.PHONE;
    }

    /**
     * Normalizes a phone string by stripping common formatting characters and valid Indian prefixes.
     *
     * @param value raw phone string
     * @return normalized 10-digit string, or null if invalid
     */
    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.trim().replaceAll("[\\s\\-\\(\\)]", "");

        if (clean.startsWith("+91")) {
            clean = clean.substring(3);
        } else if (clean.startsWith("91") && clean.length() == 12) {
            clean = clean.substring(2);
        } else if (clean.startsWith("0") && clean.length() == 11) {
            clean = clean.substring(1);
        }

        return clean;
    }

    @Override
    public boolean matchesFormat(String value) {
        if (value == null) {
            return false;
        }
        String normalized = normalize(value);
        if (normalized == null || normalized.length() != 10) {
            return false;
        }
        return TEN_DIGIT_INDIAN_MOBILE.matcher(normalized).matches();
    }

    @Override
    public Double calculateChecksumRate(List<String> values) {
        // Mobile numbers do not use an algorithmic checksum; return null
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
