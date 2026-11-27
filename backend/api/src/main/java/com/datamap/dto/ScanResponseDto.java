package com.datamap.dto;

import java.util.List;
import java.util.UUID;

/**
 * Complete response returned upon scanning an uploaded file.
 */
public record ScanResponseDto(
    UUID scanId,
    String fileName,
    int rowCount,
    int columnCount,
    ScanSummaryDto summary,
    List<ColumnFindingDto> columns,
    String disclaimer
) {
    public static final String DEFAULT_DISCLAIMER =
        "Results are candidate matches based on format rules, not proof of identity.";
}
