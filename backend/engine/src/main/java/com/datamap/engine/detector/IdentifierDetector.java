package com.datamap.engine.detector;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.DetectionResult;
import java.util.List;

/**
 * Contract for sensitive identifier detectors.
 */
public interface IdentifierDetector {

    /**
     * @return the identifier type handled by this detector
     */
    IdentifierType getType();

    /**
     * Checks if a raw string value matches the format of this identifier.
     *
     * @param value raw cell value
     * @return true if format matches, false otherwise
     */
    boolean matchesFormat(String value);

    /**
     * Calculates the checksum pass rate for sampled values, if applicable.
     *
     * @param values list of non-empty sample strings
     * @return checksum pass rate (0.0 to 1.0) or null if detector does not use checksums
     */
    Double calculateChecksumRate(List<String> values);

    /**
     * Evaluates a collection of sampled values and returns aggregated detection statistics.
     *
     * @param sampledValues list of sampled cell strings
     * @return detection result with match rates and counts
     */
    DetectionResult evaluate(List<String> sampledValues);
}
