package com.datamap.engine.detector;

import com.datamap.engine.checksum.VerhoeffChecksum;
import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.DetectionResult;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detector for Indian Aadhaar numbers.
 *
 * <p>Format rules:
 * - Exactly 12 digits
 * - First digit must be between 2 and 9
 * - Optional spaces or hyphens between 4-digit groups (e.g. 2345 6789 0124 or 2345-6789-0124)
 * - Verhoeff checksum algorithm validation
 * </p>
 */
public class AadhaarDetector implements IdentifierDetector {

    // 12 digits, first digit 2-9, optional single space or hyphen after 4th and 8th digits
    private static final Pattern AADHAAR_FORMAT_PATTERN =
        Pattern.compile("^[2-9]\\d{3}[ -]?\\d{4}[ -]?\\d{4}$");

    private static final Pattern TWELVE_DIGITS_PATTERN =
        Pattern.compile("^[2-9]\\d{11}$");

    @Override
    public IdentifierType getType() {
        return IdentifierType.AADHAAR;
    }

    @Override
    public boolean matchesFormat(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return AADHAAR_FORMAT_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Validates whether a value matches format AND passes the Verhoeff checksum.
     */
    public boolean isChecksumValid(String value) {
        if (!matchesFormat(value)) {
            return false;
        }
        String digits = value.replaceAll("\\D", "");
        return TWELVE_DIGITS_PATTERN.matcher(digits).matches() && VerhoeffChecksum.validate(digits);
    }

    @Override
    public Double calculateChecksumRate(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        int formatMatchedCount = 0;
        int checksumPassedCount = 0;

        for (String val : values) {
            if (matchesFormat(val)) {
                formatMatchedCount++;
                String digits = val.replaceAll("\\D", "");
                if (TWELVE_DIGITS_PATTERN.matcher(digits).matches() && VerhoeffChecksum.validate(digits)) {
                    checksumPassedCount++;
                }
            }
        }

        if (formatMatchedCount == 0) {
            return 0.0;
        }
        return (double) checksumPassedCount / formatMatchedCount;
    }

    @Override
    public DetectionResult evaluate(List<String> sampledValues) {
        if (sampledValues == null || sampledValues.isEmpty()) {
            return new DetectionResult(getType(), 0.0, null, 0, 0);
        }

        int total = sampledValues.size();
        int matched = 0;
        int checksumPassed = 0;

        for (String val : sampledValues) {
            if (matchesFormat(val)) {
                matched++;
                String digits = val.replaceAll("\\D", "");
                if (TWELVE_DIGITS_PATTERN.matcher(digits).matches() && VerhoeffChecksum.validate(digits)) {
                    checksumPassed++;
                }
            }
        }

        double matchRate = (double) matched / total;
        Double checksumRate = matched > 0 ? ((double) checksumPassed / matched) : 0.0;

        return new DetectionResult(getType(), matchRate, checksumRate, matched, total);
    }
}
