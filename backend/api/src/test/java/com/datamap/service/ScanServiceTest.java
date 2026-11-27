package com.datamap.service;

import com.datamap.dto.ScanResponseDto;
import com.datamap.engine.ColumnScanner;
import com.datamap.engine.DefaultColumnScanner;
import com.datamap.engine.enums.IdentifierType;
import com.datamap.enums.ErrorCode;
import com.datamap.exception.DataMapException;
import com.datamap.parser.CsvParser;
import com.datamap.parser.ParserFactory;
import com.datamap.parser.XlsxParser;
import com.datamap.repository.ScanRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScanServiceTest {

    @Mock
    private ScanRepository scanRepository;

    private ScanService scanService;

    @BeforeEach
    void setUp() {
        CsvParser csvParser = new CsvParser();
        XlsxParser xlsxParser = new XlsxParser();
        ParserFactory parserFactory = new ParserFactory(csvParser, xlsxParser);
        ColumnScanner scanner = new DefaultColumnScanner();

        scanService = new ScanService(parserFactory, scanner, scanRepository);
    }

    @Test
    @DisplayName("Scans valid CSV, flags sensitive columns, and saves metadata")
    void testScanCsv() {
        String csv = "Name,Phone,Email\nRahul,9876543210,rahul@example.com\nVikram,8765432109,vikram@example.com";
        MockMultipartFile file = new MockMultipartFile(
            "file", "candidates.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        ScanResponseDto response = scanService.scanFile(file);

        assertThat(response.fileName()).isEqualTo("candidates.csv");
        assertThat(response.rowCount()).isEqualTo(2);
        assertThat(response.columnCount()).isEqualTo(3);
        assertThat(response.summary().flaggedColumns()).isEqualTo(2);

        verify(scanRepository).save(any());
    }

    @Test
    @DisplayName("Scans valid XLSX file and identifies sensitive identifiers")
    void testScanXlsx() throws IOException {
        byte[] xlsxBytes = buildXlsx("Mobile", "9876543210");
        MockMultipartFile file = new MockMultipartFile(
            "file", "records.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes
        );

        ScanResponseDto response = scanService.scanFile(file);

        assertThat(response.fileName()).isEqualTo("records.xlsx");
        assertThat(response.summary().flaggedColumns()).isEqualTo(1);
        assertThat(response.columns().get(0).type()).isEqualTo(IdentifierType.PHONE);
    }

    @Test
    @DisplayName("Rejects empty file with EMPTY_FILE error")
    void testEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> scanService.scanFile(file))
            .isInstanceOf(DataMapException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMPTY_FILE);
    }

    @Test
    @DisplayName("Rejects unsupported file format with UNSUPPORTED_FILE_TYPE error")
    void testUnsupportedFormat() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "data.json", "application/json", "{}".getBytes()
        );

        assertThatThrownBy(() -> scanService.scanFile(file))
            .isInstanceOf(DataMapException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    @DisplayName("Masks selected columns and preserves unmasked data")
    void testMaskColumns() {
        String csv = "Name,Phone\nRahul,9876543210";
        MockMultipartFile file = new MockMultipartFile(
            "file", "data.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        byte[] maskedBytes = scanService.maskFile(file, List.of(1));
        String result = new String(maskedBytes, StandardCharsets.UTF_8);

        assertThat(result).contains("Name,Phone");
        assertThat(result).contains("Rahul,XXXXXX3210");
    }

    @Test
    @DisplayName("Throws error on invalid column index selection")
    void testInvalidColumnSelection() {
        String csv = "Name,Phone\nRahul,9876543210";
        MockMultipartFile file = new MockMultipartFile(
            "file", "data.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> scanService.maskFile(file, List.of(5)))
            .isInstanceOf(DataMapException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARSE_FAILED);
    }

    private byte[] buildXlsx(String header, String val) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Data");
            Row r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue(header);
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(val);
            wb.write(bos);
            return bos.toByteArray();
        }
    }
}
