package com.datamap.engine;

import com.datamap.engine.model.ColumnFinding;
import java.util.List;

/**
 * Public contract for scanning file columns and detecting sensitive identifiers.
 */
public interface ColumnScanner {

    /**
     * Scans tabular data by column and identifies potential sensitive data.
     *
     * @param headers list of column header names
     * @param rows list of data rows (each row is a list of cell values)
     * @return list of column findings for flagged columns
     */
    List<ColumnFinding> scan(
        List<String> headers,
        List<List<String>> rows
    );
}
