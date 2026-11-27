package com.datamap.dto;

/**
 * Standard API error response payload.
 */
public record ErrorResponseDto(
    String error,
    String message
) {}
