package com.datamap.parser;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.enums.ErrorCode;
import com.datamap.exception.DataMapException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvParserTest {

    private CsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new CsvParser();
    }

    @Test
    @DisplayName("Parses standard tabular CSV with headers and data rows")
    void testParseValidCsv() {
        String csv = "Name,Phone,Email\nAnil,9876543210,anil@test.com\nSunil,9123456789,sunil@test.com";
        ByteArrayInputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ParsedFile parsed = parser.parse(input);

        assertThat(parsed.headers()).containsExactly("Name", "Phone", "Email");
        assertThat(parsed.rowCount()).isEqualTo(2);
        assertThat(parsed.rows().get(0)).containsExactly("Anil", "9876543210", "anil@test.com");
    }

    @Test
    @DisplayName("Throws exception when CSV has no data rows")
    void testNoDataRows() {
        String csv = "Name,Phone,Email\n";
        ByteArrayInputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> parser.parse(input))
            .isInstanceOf(DataMapException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARSE_FAILED);
    }

    @Test
    @DisplayName("Writes masked CSV preserving headers and unmasked columns")
    void testWriteMaskedCsv() {
        String csv = "Name,Phone\nAnil,9876543210";
        ByteArrayInputStream input = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        parser.writeMasked(input, output, Map.of(1, IdentifierType.PHONE));

        String result = output.toString(StandardCharsets.UTF_8);
        assertThat(result).contains("Name,Phone");
        assertThat(result).contains("Anil,XXXXXX3210");
    }
}
