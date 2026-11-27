package com.datamap.parser;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.enums.ErrorCode;
import com.datamap.exception.DataMapException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XlsxParserTest {

    private XlsxParser parser;

    @BeforeEach
    void setUp() {
        parser = new XlsxParser();
    }

    @Test
    @DisplayName("Parses XLSX workbook and prevents numeric identifiers from converting to scientific notation")
    void testParseValidXlsx() throws IOException {
        byte[] bytes = createSampleWorkbook("Card Number", "987654321012");
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);

        ParsedFile parsed = parser.parse(input);

        assertThat(parsed.headers()).containsExactly("Card Number");
        assertThat(parsed.rowCount()).isEqualTo(1);
        assertThat(parsed.rows().get(0).get(0)).isEqualTo("987654321012");
    }

    @Test
    @DisplayName("Throws exception when XLSX workbook has no data rows")
    void testNoDataRows() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Header1");
            wb.write(bos);

            ByteArrayInputStream input = new ByteArrayInputStream(bos.toByteArray());
            assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(DataMapException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARSE_FAILED);
        }
    }

    @Test
    @DisplayName("Writes masked XLSX workbook correctly")
    void testWriteMaskedXlsx() throws IOException {
        byte[] bytes = createSampleWorkbook("Aadhaar", "234567890124");
        ByteArrayInputStream input = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        parser.writeMasked(input, output, Map.of(0, IdentifierType.AADHAAR));

        ParsedFile maskedParsed = parser.parse(new ByteArrayInputStream(output.toByteArray()));
        assertThat(maskedParsed.rows().get(0).get(0)).isEqualTo("XXXX-XXXX-0124");
    }

    private byte[] createSampleWorkbook(String header, String cellValue) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("TestSheet");
            Row r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue(header);

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(cellValue);

            wb.write(bos);
            return bos.toByteArray();
        }
    }
}
