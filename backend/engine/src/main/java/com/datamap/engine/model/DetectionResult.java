package com.datamap.engine.model;

import com.datamap.engine.enums.IdentifierType;

/**
 * Result of evaluating sample values against a specific identifier detector.
 */
public record DetectionResult(
    IdentifierType type,
    double matchRate,
    Double checksumRate,
    int matchedCount,
    int totalSampled
) {}
