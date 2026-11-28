package com.datamap.parser;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.masker.Masker;
import com.datamap.enums.ErrorCode;
import com.datamap.exception.DataMapException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * XLSX parser and exporter implemented using Apache POI.
 *
 * <p>Supports single-sheet workbooks only.
 * Reads cell values as strings preserving leading zeros and avoiding scientific notation.</p>
 */
@Component
public class XlsxParser implements FileParser {

    @Override
    public ParsedFile parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new DataMapException(ErrorCode.EMPTY_FILE, "File is empty or missing");
        }

        DataFormatter dataFormatter = new DataFormatter();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            int sheetCount = workbook.getNumberOfSheets();
            if (sheetCount == 0) {
                throw new DataMapException(ErrorCode.EMPTY_FILE, "Workbook contains no sheets");
            }
            if (sheetCount > 1) {
                throw new DataMapException(ErrorCode.PARSE_FAILED, "Only single-sheet workbooks are supported");
            }

            XSSFSheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            if (lastRowNum < 0 || sheet.getPhysicalNumberOfRows() == 0) {
                throw new DataMapException(ErrorCode.NO_HEADER_ROW, "File contains no header row");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null || headerRow.getPhysicalNumberOfCells() == 0) {
                throw new DataMapException(ErrorCode.NO_HEADER_ROW, "File contains no header row");
            }

            int lastCellNum = headerRow.getLastCellNum();
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < lastCellNum; c++) {
                Cell cell = headerRow.getCell(c);
                headers.add(getCellValueAsString(cell, dataFormatter));
            }

            if (headers.isEmpty() || headers.stream().allMatch(String::isBlank)) {
                throw new DataMapException(ErrorCode.NO_HEADER_ROW, "File contains no header row");
            }

            List<List<String>> rows = new ArrayList<>();
            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                List<String> rowValues = new ArrayList<>();
                for (int c = 0; c < lastCellNum; c++) {
                    Cell cell = row.getCell(c);
                    rowValues.add(getCellValueAsString(cell, dataFormatter));
                }
                rows.add(rowValues);
            }

            if (rows.isEmpty()) {
                throw new DataMapException(ErrorCode.PARSE_FAILED, "File contains no data rows");
            }

            return new ParsedFile(headers, rows);
        } catch (DataMapException e) {
            throw e;
        } catch (Exception e) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "Failed to parse XLSX file: " + e.getMessage());
        }
    }

    @Override
    public void writeMasked(
        ParsedFile parsed,
        OutputStream outputStream,
        Map<Integer, IdentifierType> columnsToMask,
        Set<Integer> genericMaskColumns
    ) {
        try (XSSFWorkbook newWorkbook = new XSSFWorkbook()) {
            XSSFSheet sheet = newWorkbook.createSheet("Masked Data");

            // Write sanitized header row
            Row headerRow = sheet.createRow(0);
            List<String> headers = parsed.headers();
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = headerRow.createCell(c, CellType.STRING);
                cell.setCellValue(FormulaSanitizer.sanitizeCell(headers.get(c)));
            }

            // Write data rows with masked columns and formula-injection neutralization
            List<List<String>> rows = parsed.rows();
            for (int r = 0; r < rows.size(); r++) {
                Row dataRow = sheet.createRow(r + 1);
                List<String> rowValues = rows.get(r);
                for (int c = 0; c < headers.size(); c++) {
                    String val = (c < rowValues.size()) ? rowValues.get(c) : "";
                    Cell cell = dataRow.createCell(c, CellType.STRING);
                    String outVal;
                    if (columnsToMask != null && columnsToMask.containsKey(c)) {
                        outVal = Masker.mask(val, columnsToMask.get(c));
                    } else if (genericMaskColumns != null && genericMaskColumns.contains(c)) {
                        outVal = Masker.maskGeneric(val);
                    } else {
                        outVal = val != null ? val : "";
                    }
                    cell.setCellValue(FormulaSanitizer.sanitizeCell(outVal));
                }
            }

            newWorkbook.write(outputStream);
        } catch (Exception e) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "Failed to write masked XLSX file: " + e.getMessage());
        }
    }

    /**
     * Reads cell value as plain string, avoiding scientific notation for large numbers
     * and preserving exact text formatting.
     */
    private String getCellValueAsString(Cell cell, DataFormatter dataFormatter) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield dataFormatter.formatCellValue(cell);
                }
                double num = cell.getNumericCellValue();
                // Prevent scientific notation for large integers (such as Aadhaar or mobile numbers)
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    yield BigDecimal.valueOf((long) num).toPlainString();
                }
                yield BigDecimal.valueOf(num).stripTrailingZeros().toPlainString();
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> dataFormatter.formatCellValue(cell);
            case BLANK -> "";
            default -> dataFormatter.formatCellValue(cell);
        };
    }
}
