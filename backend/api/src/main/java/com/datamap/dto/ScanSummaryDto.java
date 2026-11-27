package com.datamap.dto;

/**
 * Summary counts of flagged columns by confidence level.
 */
public record ScanSummaryDto(
    int flaggedColumns,
    int high,
    int medium,
    int low
) {}
