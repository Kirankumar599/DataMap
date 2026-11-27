package com.datamap.engine;

import com.datamap.engine.checksum.VerhoeffChecksum;
import com.datamap.engine.enums.Confidence;
import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.ColumnFinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ColumnScannerTest {

    private ColumnScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new DefaultColumnScanner();
    }

    @Test
    @DisplayName("Scans tabular data and accurately detects multiple sensitive columns")
    void testFullScan() {
        List<String> headers = List.of("ID", "Student Name", "Aadhaar No", "PAN", "Mobile", "Email", "Marks");

        // Synthesize valid Aadhaar numbers
        String aadh1Base = "23456789012";
        String aadh1 = aadh1Base + VerhoeffChecksum.calculateCheckDigit(aadh1Base);
        String aadh2Base = "34567890123";
        String aadh2 = aadh2Base + VerhoeffChecksum.calculateCheckDigit(aadh2Base);

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("1", "Ravi Kumar", aadh1, "ABCPK1234F", "9876543210", "ravi.k@gmail.com", "95"));
        rows.add(List.of("2", "Priya Sharma", aadh2, "XYZCA5678G", "+91 8888877777", "priya.s@yahoo.com", "88"));
        rows.add(List.of("3", "Amit Verma", "N/A", "MNPHB9012K", "07777766666", "amit.v@outlook.com", "92"));

        List<ColumnFinding> findings = scanner.scan(headers, rows);

        // Expect 4 flagged columns: Aadhaar (index 2), PAN (index 3), Mobile (index 4), Email (index 5)
        assertThat(findings).hasSize(4);

        ColumnFinding aadhaarFinding = findings.stream().filter(f -> f.type() == IdentifierType.AADHAAR).findFirst().orElseThrow();
        assertThat(aadhaarFinding.columnIndex()).isEqualTo(2);
        assertThat(aadhaarFinding.confidence()).isIn(Confidence.HIGH, Confidence.MEDIUM);
        assertThat(aadhaarFinding.checksumRate()).isNotNull();
        assertThat(aadhaarFinding.maskedSamples()).isNotEmpty();
        assertThat(aadhaarFinding.maskedSamples().get(0)).startsWith("XXXX-XXXX-");

        ColumnFinding panFinding = findings.stream().filter(f -> f.type() == IdentifierType.PAN).findFirst().orElseThrow();
        assertThat(panFinding.columnIndex()).isEqualTo(3);
        assertThat(panFinding.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(panFinding.maskedSamples().get(0)).startsWith("XXXXXX");

        ColumnFinding phoneFinding = findings.stream().filter(f -> f.type() == IdentifierType.PHONE).findFirst().orElseThrow();
        assertThat(phoneFinding.columnIndex()).isEqualTo(4);
        assertThat(phoneFinding.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(phoneFinding.maskedSamples().get(0)).startsWith("XXXXXX");

        ColumnFinding emailFinding = findings.stream().filter(f -> f.type() == IdentifierType.EMAIL).findFirst().orElseThrow();
        assertThat(emailFinding.columnIndex()).isEqualTo(5);
        assertThat(emailFinding.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(emailFinding.maskedSamples().get(0)).contains("***@");

        // Non-sensitive columns (ID, Student Name, Marks) should not be flagged
        assertThat(findings.stream().anyMatch(f -> f.columnIndex() == 0)).isFalse();
        assertThat(findings.stream().anyMatch(f -> f.columnIndex() == 1)).isFalse();
        assertThat(findings.stream().anyMatch(f -> f.columnIndex() == 6)).isFalse();
    }
}
