package com.datamap.engine.detector;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.DetectionResult;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detector for email addresses.
 *
 * <p>Format rules:
 * - Practical email pattern: [local]@[domain].[tld]
 * - Reject consecutive dots, leading/trailing dots in local part
 * - Reject blank, null, or excessively long values (>254 chars)
 * </p>
 */
public class EmailDetector implements IdentifierDetector {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Override
    public IdentifierType getType() {
        return IdentifierType.EMAIL;
    }

    @Override
    public boolean matchesFormat(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 254) {
            return false;
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            return false;
        }

        // Additional sanity checks
        if (trimmed.contains("..")) {
            return false;
        }
        int atIndex = trimmed.indexOf('@');
        String local = trimmed.substring(0, atIndex);
        if (local.startsWith(".") || local.endsWith(".")) {
            return false;
        }
        String domain = trimmed.substring(atIndex + 1);
        if (domain.startsWith(".") || domain.endsWith(".")) {
            return false;
        }

        return true;
    }

    @Override
    public Double calculateChecksumRate(List<String> values) {
        // Emails do not use an algorithmic checksum; return null
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
