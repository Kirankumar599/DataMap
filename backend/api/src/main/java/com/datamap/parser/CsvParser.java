package com.datamap.parser;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.masker.Masker;
import com.datamap.enums.ErrorCode;
import com.datamap.exception.DataMapException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CSV parser and exporter implemented using Apache Commons CSV.
 */
@Component
public class CsvParser implements FileParser {

    @Override
    public ParsedFile parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new DataMapException(ErrorCode.EMPTY_FILE, "File is empty or missing");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            CSVParser csvParser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(false)
                .setTrim(false)
                .build()
                .parse(reader);

            List<String> headers = csvParser.getHeaderNames();
            if (headers == null || headers.isEmpty() || headers.stream().allMatch(String::isBlank)) {
                throw new DataMapException(ErrorCode.NO_HEADER_ROW, "File contains no header row");
            }

            List<List<String>> rows = new ArrayList<>();
            for (CSVRecord record : csvParser) {
                List<String> row = new ArrayList<>();
                for (String val : record) {
                    row.add(val);
                }
                rows.add(row);
            }

            if (rows.isEmpty()) {
                throw new DataMapException(ErrorCode.PARSE_FAILED, "File contains no data rows");
            }

            return new ParsedFile(headers, rows);
        } catch (DataMapException e) {
            throw e;
        } catch (Exception e) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "Failed to parse CSV file: " + e.getMessage());
        }
    }

    @Override
    public void writeMasked(
        InputStream originalInputStream,
        OutputStream outputStream,
        Map<Integer, IdentifierType> columnsToMask
    ) {
        ParsedFile parsed = parse(originalInputStream);

        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            // Write original headers
            printer.printRecord(parsed.headers());

            // Write rows with selected columns masked
            for (List<String> row : parsed.rows()) {
                List<String> maskedRow = new ArrayList<>(row.size());
                for (int i = 0; i < row.size(); i++) {
                    String val = row.get(i);
                    if (columnsToMask != null && columnsToMask.containsKey(i)) {
                        maskedRow.add(Masker.mask(val, columnsToMask.get(i)));
                    } else {
                        maskedRow.add(val);
                    }
                }
                printer.printRecord(maskedRow);
            }
            printer.flush();
        } catch (Exception e) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "Failed to write masked CSV file: " + e.getMessage());
        }
    }
}
