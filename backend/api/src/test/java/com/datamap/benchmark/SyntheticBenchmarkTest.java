package com.datamap.benchmark;

import com.datamap.engine.ColumnScanner;
import com.datamap.engine.DefaultColumnScanner;
import com.datamap.engine.checksum.VerhoeffChecksum;
import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.ColumnFinding;
import com.datamap.enums.ErrorCode;
import com.datamap.exception.DataMapException;
import com.datamap.parser.CsvParser;
import com.datamap.parser.ParsedFile;
import com.datamap.parser.XlsxParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 20-case synthetic accuracy benchmark and near-10MB latency harness.
 * Uses 100% synthetic values only.
 */
class SyntheticBenchmarkTest {

    private final CsvParser csvParser = new CsvParser();
    private final XlsxParser xlsxParser = new XlsxParser();
    private final ColumnScanner scanner = new DefaultColumnScanner();

    private record AnswerKey(
        String fileName,
        boolean isXlsx,
        byte[] content,
        Map<Integer, IdentifierType> expectedColumns
    ) {}

    @Test
    @DisplayName("Runs 20-file synthetic accuracy benchmark (CSV + XLSX + near-10MB + edge cases) and measures Precision/Recall")
    void runTwentyFileSyntheticBenchmark() throws IOException {
        List<AnswerKey> suite = buildSyntheticSuite();

        Map<IdentifierType, Integer> planted = new EnumMap<>(IdentifierType.class);
        Map<IdentifierType, Integer> truePositive = new EnumMap<>(IdentifierType.class);
        Map<IdentifierType, Integer> falsePositive = new EnumMap<>(IdentifierType.class);
        for (IdentifierType t : IdentifierType.values()) {
            planted.put(t, 0);
            truePositive.put(t, 0);
            falsePositive.put(t, 0);
        }

        long near10MbDurationMs = 0;
        int near10MbBytes = 0;

        for (AnswerKey item : suite) {
            long start = System.currentTimeMillis();
            ParsedFile parsed = item.isXlsx()
                ? xlsxParser.parse(new ByteArrayInputStream(item.content()))
                : csvParser.parse(new ByteArrayInputStream(item.content()));
            List<ColumnFinding> findings = scanner.scan(parsed.headers(), parsed.rows());
            long elapsed = System.currentTimeMillis() - start;

            if (item.fileName().contains("near_10mb")) {
                near10MbDurationMs = elapsed;
                near10MbBytes = item.content().length;
            }

            // Tally planted columns
            for (IdentifierType t : item.expectedColumns().values()) {
                planted.merge(t, 1, Integer::sum);
            }

            // Tally predicted columns
            Map<Integer, IdentifierType> predicted = new HashMap<>();
            for (ColumnFinding f : findings) {
                predicted.put(f.columnIndex(), f.type());
            }

            for (Map.Entry<Integer, IdentifierType> pred : predicted.entrySet()) {
                IdentifierType expectedType = item.expectedColumns().get(pred.getKey());
                if (expectedType == pred.getValue()) {
                    truePositive.merge(pred.getValue(), 1, Integer::sum);
                } else {
                    falsePositive.merge(pred.getValue(), 1, Integer::sum);
                }
            }
        }

        // Verify edge-case suite (file #20: empty, header-only, multi-sheet)
        assertThatThrownBy(() -> csvParser.parse(new ByteArrayInputStream(new byte[0])))
            .isInstanceOf(DataMapException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_HEADER_ROW);
        assertThatThrownBy(() -> csvParser.parse(new ByteArrayInputStream("ColA,ColB\n".getBytes(StandardCharsets.UTF_8))))
            .isInstanceOf(DataMapException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARSE_FAILED);

        StringBuilder report = new StringBuilder();
        report.append("\n=== SYNTHETIC BENCHMARK RESULTS (19 tabular files + edge-case bundle) ===\n");
        report.append(String.format("%-10s | %-7s | %-5s | %-11s | %-9s | %-6s%n",
            "Type", "Planted", "Found", "False Flags", "Precision", "Recall"));
        report.append("---------------------------------------------------------------------\n");

        for (IdentifierType t : IdentifierType.values()) {
            int p = planted.get(t);
            int tp = truePositive.get(t);
            int fp = falsePositive.get(t);
            double precision = (tp + fp) > 0 ? (100.0 * tp / (tp + fp)) : 100.0;
            double recall = p > 0 ? (100.0 * tp / p) : 100.0;
            report.append(String.format("%-10s | %-7d | %-5d | %-11d | %8.1f%% | %5.1f%%%n",
                t.name(), p, tp, fp, precision, recall));

            // Every planted column must be found (100% recall)
            assertThat(tp).as("Recall for " + t).isEqualTo(p);
        }

        report.append(String.format("Near-10MB file (%d bytes / %.2f MB) parsed + scanned in %d ms%n",
            near10MbBytes, near10MbBytes / (1024.0 * 1024.0), near10MbDurationMs));
        System.out.println(report);
        assertThat(near10MbBytes).isBetween(8_000_000, 10 * 1024 * 1024);

        // Also export demo sample file to repo-root samples/college_admissions.xlsx
        Path samplesDir = Path.of("..", "..", "samples");
        Files.createDirectories(samplesDir);
        Files.write(samplesDir.resolve("college_admissions.xlsx"), suite.get(0).content());
    }

    private List<AnswerKey> buildSyntheticSuite() throws IOException {
        List<AnswerKey> suite = new ArrayList<>();

        // 1. Demo flagship: college_admissions.xlsx (messy headers + roll no hard negative)
        suite.add(xlsxCase("01_college_admissions.xlsx",
            List.of("Roll No", "Student Name", "Adhar", "PAN No", "Mob no", "E-mail", "Marks"),
            rows(25, i -> List.of(
                "1023456789" + String.format("%02d", i),
                "Student " + i,
                spacedAadhaar(i),
                validPan(i, 'P'),
                "+91 " + phone10(i),
                "student" + i + "@college.edu.in",
                String.valueOf(70 + (i % 30))
            )),
            Map.of(2, IdentifierType.AADHAAR, 3, IdentifierType.PAN, 4, IdentifierType.PHONE, 5, IdentifierType.EMAIL)
        ));

        // 2. Clinic patients CSV
        suite.add(csvCase("02_clinic_patients.csv",
            List.of("Patient_ID", "Adhar", "Mob no", "E-mail", "Age"),
            rows(20, i -> List.of(
                "PAT-" + (1000 + i),
                hyphenAadhaar(i + 10),
                "0" + phone10(i + 5),
                "patient" + i + "@gmail.com",
                String.valueOf(20 + (i % 50))
            )),
            Map.of(1, IdentifierType.AADHAAR, 2, IdentifierType.PHONE, 3, IdentifierType.EMAIL)
        ));

        // 3. NGO donors XLSX
        suite.add(xlsxCase("03_ngo_donors.xlsx",
            List.of("Aadhar No", "PAN Card", "Ph", "Mail", "Amount"),
            rows(15, i -> List.of(
                plainAadhaar(i + 30),
                validPan(i + 3, 'C'),
                phone10(i + 12),
                "donor." + i + "@ngo.org",
                String.valueOf(500 * (i + 1))
            )),
            Map.of(0, IdentifierType.AADHAAR, 1, IdentifierType.PAN, 2, IdentifierType.PHONE, 3, IdentifierType.EMAIL)
        ));

        // 4. Misleading generic headers (Col_A..Col_D)
        suite.add(csvCase("04_staff_payroll.csv",
            List.of("Col_A", "Col_B", "Col_C", "Col_D"),
            rows(15, i -> List.of(
                "EMP" + (100 + i),
                validPan(i + 7, 'P'),
                "+91-" + phone10(i + 20),
                "staff" + i + "@clinic.in"
            )),
            Map.of(1, IdentifierType.PAN, 2, IdentifierType.PHONE, 3, IdentifierType.EMAIL)
        ));

        // 5. Scholarship roster with UID + Remarks containing embedded emails (hard negative)
        suite.add(xlsxCase("05_scholarship_roster.xlsx",
            List.of("UID", "Mobile", "Email Address", "Remarks"),
            rows(15, i -> List.of(
                spacedAadhaar(i + 50),
                phone10(i + 30),
                "scholar" + i + "@yahoo.com",
                "Verified; contact help" + i + "@gmail.com for status"
            )),
            Map.of(0, IdentifierType.AADHAAR, 1, IdentifierType.PHONE, 2, IdentifierType.EMAIL)
        ));

        // 6. Exam center: 0-prefixed 12-digit roll numbers + random 12-digit IDs failing Verhoeff + 0-prefixed mobile
        suite.add(csvCase("06_exam_center_list.csv",
            List.of("ID", "RollNo", "Phone"),
            rows(20, i -> List.of(
                invalidVerhoeffAadhaar(i),
                "0123456789" + String.format("%02d", i),
                "0" + phone10(i + 40)
            )),
            Map.of(2, IdentifierType.PHONE)
        ));

        // 7. Mixed formatting + empty cells
        suite.add(xlsxCase("07_mixed_formatting.xlsx",
            List.of("Aadhaar", "PAN", "Contact", "Email"),
            rows(20, i -> List.of(
                i % 4 == 0 ? "" : (i % 2 == 0 ? hyphenAadhaar(i + 70) : plainAadhaar(i + 70)),
                i % 5 == 0 ? "N/A" : validPan(i + 15, 'P').toLowerCase(),
                i % 3 == 0 ? "91" + phone10(i + 50) : "+91 " + phone10(i + 50),
                i % 4 == 0 ? "" : "user" + i + "@outlook.com"
            )),
            Map.of(0, IdentifierType.AADHAAR, 1, IdentifierType.PAN, 2, IdentifierType.PHONE, 3, IdentifierType.EMAIL)
        ));

        // 8. Vendor invoices: substring trap "Company" must NOT match PAN
        suite.add(csvCase("08_vendor_invoices.csv",
            List.of("Company", "PAN No", "Mobile Number", "User Email"),
            rows(15, i -> List.of(
                "Vendor Corp " + i,
                validPan(i + 20, 'F'),
                phone10(i + 60),
                "billing" + i + "@vendor.co.in"
            )),
            Map.of(1, IdentifierType.PAN, 2, IdentifierType.PHONE, 3, IdentifierType.EMAIL)
        ));

        // 9. Alumni directory
        suite.add(xlsxCase("09_alumni_directory.xlsx",
            List.of("E-mail", "Cell", "Batch"),
            rows(15, i -> List.of(
                "alumni+" + i + "@sub.college.org",
                phone10(i + 70),
                String.valueOf(2015 + (i % 10))
            )),
            Map.of(0, IdentifierType.EMAIL, 1, IdentifierType.PHONE)
        ));

        // 10. Sparse clinic log (50% blanks)
        suite.add(csvCase("10_sparse_clinic_log.csv",
            List.of("Aadhaar Card", "Email"),
            rows(20, i -> List.of(
                i % 2 == 0 ? plainAadhaar(i + 90) : "",
                i % 2 == 0 ? "doc" + i + "@clinic.org" : ""
            )),
            Map.of(0, IdentifierType.AADHAAR, 1, IdentifierType.EMAIL)
        ));

        // 11. Zero-PII hard-negative trap (misleading headers, free-text emails, PAN-like serial codes)
        suite.add(xlsxCase("11_hard_negatives_nopii.xlsx",
            List.of("Paragraph", "Participant", "Student_Guid", "Remarks", "Product_Code"),
            rows(15, i -> List.of(
                "Section " + i,
                "Member " + i,
                "GUID-" + (1000 + i),
                "Send invoice to accounts" + i + "@example.com",
                "ABCDE" + String.format("%04d", 1000 + i) + "Z" // 4th char 'D' invalid for PAN
            )),
            Map.of()
        ));

        // 12. Numeric hard negatives: 12-digit roll no (starts 1), invalid Verhoeff 12-digit ID, 91-prefixed phones under Col_B
        suite.add(csvCase("12_hard_negatives_numeric.csv",
            List.of("Roll_12", "Random_ID", "Col_B"),
            rows(20, i -> List.of(
                "1987654321" + String.format("%02d", i),
                invalidVerhoeffAadhaar(i + 20),
                "91" + phone10(i + 80)
            )),
            Map.of(2, IdentifierType.PHONE)
        ));

        // 13. Malformed emails & short numbers
        suite.add(csvCase("13_malformed_emails_and_ids.csv",
            List.of("Notes", "Short_Ref", "Contact Email"),
            rows(15, i -> List.of(
                "bad..email" + i + "@gmail.com",
                "9876543" + String.format("%02d", i), // 9 digits -> not phone
                "valid" + i + "@domain.com"
            )),
            Map.of(2, IdentifierType.EMAIL)
        ));

        // 14. Lowercase PAN vs serial codes
        suite.add(xlsxCase("14_pan_lowercase_and_serials.xlsx",
            List.of("Tax_ID", "Serial_No"),
            rows(15, i -> List.of(
                validPan(i + 35, 'P').toLowerCase(),
                "QWERT" + String.format("%04d", 2000 + i) + "Y" // 4th char 'R' invalid
            )),
            Map.of(0, IdentifierType.PAN)
        ));

        // 15. Corrupted 12-digit IDs vs valid Aadhaar
        suite.add(csvCase("15_aadhaar_checksum_filter.csv",
            List.of("Aadhaar", "Ref_Code"),
            rows(20, i -> List.of(
                plainAadhaar(i + 120),
                invalidVerhoeffAadhaar(i + 40)
            )),
            Map.of(0, IdentifierType.AADHAAR)
        ));

        // 16. Hospital OPD XLSX
        suite.add(xlsxCase("16_hospital_opd.xlsx",
            List.of("Adhar", "Phone", "Remarks"),
            rows(15, i -> List.of(
                spacedAadhaar(i + 140),
                "+91 " + phone10(i + 90),
                "Follow up with nurse" + i + "@hospital.in"
            )),
            Map.of(0, IdentifierType.AADHAAR, 1, IdentifierType.PHONE)
        ));

        // 17. NGO volunteers CSV
        suite.add(csvCase("17_ngo_volunteers.csv",
            List.of("pan no", "mob", "user email"),
            rows(15, i -> List.of(
                validPan(i + 50, 'P'),
                phone10(i + 100),
                "vol" + i + "@charity.org"
            )),
            Map.of(0, IdentifierType.PAN, 1, IdentifierType.PHONE, 2, IdentifierType.EMAIL)
        ));

        // 18. Large 5,000-row college export
        suite.add(csvCase("18_large_college_5k.csv",
            List.of("ID", "Aadhaar No", "PAN", "Mobile", "Email"),
            rows(5000, i -> List.of(
                String.valueOf(i + 1),
                plainAadhaar(i),
                validPan(i, 'P'),
                phone10(i),
                "stu" + i + "@univ.edu"
            )),
            Map.of(1, IdentifierType.AADHAAR, 2, IdentifierType.PAN, 3, IdentifierType.PHONE, 4, IdentifierType.EMAIL)
        ));

        // 19. Near-10MB stress CSV (~9.1 MB, within 10MB upload limit)
        suite.add(csvCase("19_near_10mb_stress.csv",
            List.of("ID", "Name", "Aadhaar", "PAN", "Mobile", "Email"),
            rows(95000, i -> List.of(
                String.valueOf(i + 1),
                "Candidate Full Name " + i,
                spacedAadhaar(i % 500),
                validPan(i % 500, 'P'),
                phone10(i % 500),
                "candidate" + (i % 5000) + "@university.edu.in"
            )),
            Map.of(2, IdentifierType.AADHAAR, 3, IdentifierType.PAN, 4, IdentifierType.PHONE, 5, IdentifierType.EMAIL)
        ));

        return suite;
    }

    private interface RowGen {
        List<String> make(int index);
    }

    private List<List<String>> rows(int count, RowGen gen) {
        List<List<String>> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(gen.make(i));
        }
        return out;
    }

    private AnswerKey csvCase(String name, List<String> headers, List<List<String>> rows, Map<Integer, IdentifierType> expected) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", headers)).append('\n');
        for (List<String> r : rows) {
            for (int c = 0; c < r.size(); c++) {
                if (c > 0) sb.append(',');
                String val = r.get(c);
                if (val.contains(",") || val.contains("\"")) {
                    sb.append('"').append(val.replace("\"", "\"\"")).append('"');
                } else {
                    sb.append(val);
                }
            }
            sb.append('\n');
        }
        return new AnswerKey(name, false, sb.toString().getBytes(StandardCharsets.UTF_8), expected);
    }

    private AnswerKey xlsxCase(String name, List<String> headers, List<List<String>> rows, Map<Integer, IdentifierType> expected) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XSSFSheet sheet = wb.createSheet("Data");
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.size(); c++) {
                headerRow.createCell(c).setCellValue(headers.get(c));
            }
            for (int r = 0; r < rows.size(); r++) {
                Row dataRow = sheet.createRow(r + 1);
                List<String> rowVals = rows.get(r);
                for (int c = 0; c < rowVals.size(); c++) {
                    dataRow.createCell(c).setCellValue(rowVals.get(c));
                }
            }
            wb.write(bos);
            return new AnswerKey(name, true, bos.toByteArray(), expected);
        }
    }

    private String plainAadhaar(int seed) {
        int first = 2 + (seed % 8);
        String base11 = first + String.format("%010d", 1000000000L + (seed * 7919L) % 8999999999L);
        return base11 + VerhoeffChecksum.calculateCheckDigit(base11);
    }

    private String spacedAadhaar(int seed) {
        String a = plainAadhaar(seed);
        return a.substring(0, 4) + " " + a.substring(4, 8) + " " + a.substring(8, 12);
    }

    private String hyphenAadhaar(int seed) {
        String a = plainAadhaar(seed);
        return a.substring(0, 4) + "-" + a.substring(4, 8) + "-" + a.substring(8, 12);
    }

    private String invalidVerhoeffAadhaar(int seed) {
        String a = plainAadhaar(seed);
        int last = a.charAt(11) - '0';
        return a.substring(0, 11) + ((last + 1) % 10);
    }

    private String validPan(int seed, char holderType) {
        char c1 = (char) ('A' + (seed % 26));
        char c2 = (char) ('A' + ((seed * 3) % 26));
        char c3 = (char) ('A' + ((seed * 7) % 26));
        char c5 = (char) ('A' + ((seed * 11) % 26));
        char c10 = (char) ('A' + ((seed * 13) % 26));
        return "" + c1 + c2 + c3 + holderType + c5 + String.format("%04d", 1000 + (seed % 9000)) + c10;
    }

    private String phone10(int seed) {
        int first = 6 + (seed % 4);
        long rest = 100000000L + (seed * 104729L) % 899999999L;
        return first + String.format("%09d", rest);
    }
}
