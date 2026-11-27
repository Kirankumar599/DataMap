package com.datamap.engine.model;

import com.datamap.engine.enums.Confidence;
import com.datamap.engine.enums.IdentifierType;
import java.util.List;

/**
 * Represents the detection finding for a single scanned column.
 *
 * @param columnIndex 0-based column index
 * @param columnName original column header name
 * @param type detected identifier type
 * @param confidence confidence level (HIGH, MEDIUM, LOW)
 * @param reasons human-readable explanations of detection signals
 * @param matchRate percentage of sampled values matching format (0.0 to 1.0)
 * @param checksumRate percentage of sampled values passing checksum (null if not applicable)
 * @param maskedSamples preview of masked sample values
 */
public record ColumnFinding(
    int columnIndex,
    String columnName,
    IdentifierType type,
    Confidence confidence,
    List<String> reasons,
    double matchRate,
    Double checksumRate,
    List<String> maskedSamples
) {}
