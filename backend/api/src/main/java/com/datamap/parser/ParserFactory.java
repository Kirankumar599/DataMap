package com.datamap.parser;

import com.datamap.enums.ErrorCode;
import com.datamap.exception.DataMapException;
import org.springframework.stereotype.Component;

/**
 * Factory for resolving file parsers based on filename and MIME type.
 */
@Component
public class ParserFactory {

    private final CsvParser csvParser;
    private final XlsxParser xlsxParser;

    public ParserFactory(CsvParser csvParser, XlsxParser xlsxParser) {
        this.csvParser = csvParser;
        this.xlsxParser = xlsxParser;
    }

    /**
     * Resolves the appropriate parser for the given filename.
     *
     * @param fileName uploaded filename
     * @return FileParser instance
     */
    public FileParser getParser(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new DataMapException(ErrorCode.UNSUPPORTED_FILE_TYPE, "Filename is missing");
        }

        String lower = fileName.toLowerCase().trim();
        if (lower.endsWith(".csv")) {
            return csvParser;
        } else if (lower.endsWith(".xlsx")) {
            return xlsxParser;
        } else if (lower.endsWith(".xls")) {
            throw new DataMapException(ErrorCode.UNSUPPORTED_FILE_TYPE, "Legacy .xls format is not supported. Please upload .xlsx");
        } else {
            throw new DataMapException(ErrorCode.UNSUPPORTED_FILE_TYPE, "Unsupported file format. Only .csv and .xlsx files are supported.");
        }
    }
}
