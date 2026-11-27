package com.datamap.parser;

import com.datamap.engine.enums.IdentifierType;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Contract for parsing and exporting tabular files (CSV, XLSX).
 */
public interface FileParser {

    /**
     * Parses an input stream into headers and data rows.
     */
    ParsedFile parse(InputStream inputStream);

    /**
     * Writes a masked copy of the file to the output stream.
     *
     * @param originalInputStream stream of the original uploaded file
     * @param outputStream target stream for the masked output file
     * @param columnsToMask map of 0-based column index to detected identifier type
     */
    void writeMasked(
        InputStream originalInputStream,
        OutputStream outputStream,
        Map<Integer, IdentifierType> columnsToMask
    );
}
