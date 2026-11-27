package com.datamap.dto;

import com.datamap.engine.enums.Confidence;
import com.datamap.engine.enums.IdentifierType;
import java.util.List;

/**
 * Finding details for a detected sensitive column.
 */
public record ColumnFindingDto(
    int index,
    String name,
    IdentifierType type,
    Confidence confidence,
    List<String> reasons,
    List<String> maskedSamples
) {}
