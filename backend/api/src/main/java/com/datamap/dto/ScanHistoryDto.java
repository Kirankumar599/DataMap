package com.datamap.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metadata-only scan history record.
 */
public record ScanHistoryDto(
    UUID id,
    String fileName,
    String fileType,
    int rowCount,
    int columnCount,
    int flaggedColumns,
    LocalDateTime createdAt,
    int durationMs
) {}
