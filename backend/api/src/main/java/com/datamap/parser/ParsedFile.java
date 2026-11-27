package com.datamap.parser;

import java.util.List;

/**
 * Common internal representation of parsed tabular files.
 */
public record ParsedFile(
    List<String> headers,
    List<List<String>> rows
) {
    public int rowCount() {
        return rows != null ? rows.size() : 0;
    }

    public int columnCount() {
        return headers != null ? headers.size() : 0;
    }
}
