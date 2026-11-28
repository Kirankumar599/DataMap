package com.datamap.service;

import com.datamap.dto.ColumnFindingDto;
import com.datamap.dto.ScanHistoryDto;
import com.datamap.dto.ScanResponseDto;
import com.datamap.dto.ScanSummaryDto;
import com.datamap.engine.ColumnScanner;
import com.datamap.engine.enums.Confidence;
import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.header.HeaderMatcher;
import com.datamap.engine.model.ColumnFinding;
import com.datamap.entity.ColumnFindingEntity;
import com.datamap.entity.ScanEntity;
import com.datamap.enums.ErrorCode;
import com.datamap.exception.DataMapException;
import com.datamap.parser.FileParser;
import com.datamap.parser.ParsedFile;
import com.datamap.parser.ParserFactory;
import com.datamap.repository.ScanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Core orchestration service for scanning files and generating masked exports.
 * Follows strict privacy constraints: NEVER logs or persists raw cell data.
 */
@Service
public class ScanService {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    private final ParserFactory parserFactory;
    private final ColumnScanner columnScanner;
    private final ScanRepository scanRepository;
    private final HeaderMatcher headerMatcher = new HeaderMatcher();

    public ScanService(
        ParserFactory parserFactory,
        ColumnScanner columnScanner,
        ScanRepository scanRepository
    ) {
        this.parserFactory = parserFactory;
        this.columnScanner = columnScanner;
        this.scanRepository = scanRepository;
    }

    /**
     * Scans an uploaded file for sensitive Indian identifiers.
     */
    @Transactional
    public ScanResponseDto scanFile(MultipartFile file) {
        validateFile(file);

        String safeFilename = sanitizeFileName(file.getOriginalFilename());
        FileParser parser = parserFactory.getParser(safeFilename);
        String fileType = getFileType(safeFilename);

        long startTime = System.currentTimeMillis();

        ParsedFile parsed;
        try {
            parsed = parser.parse(file.getInputStream());
        } catch (DataMapException e) {
            throw e;
        } catch (Exception e) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "Failed to read uploaded file: " + e.getMessage());
        }

        List<ColumnFinding> findings = columnScanner.scan(parsed.headers(), parsed.rows());

        int durationMs = (int) (System.currentTimeMillis() - startTime);

        int high = (int) findings.stream().filter(f -> f.confidence() == Confidence.HIGH).count();
        int medium = (int) findings.stream().filter(f -> f.confidence() == Confidence.MEDIUM).count();
        int low = (int) findings.stream().filter(f -> f.confidence() == Confidence.LOW).count();
        ScanSummaryDto summary = new ScanSummaryDto(findings.size(), high, medium, low);

        UUID scanId = UUID.randomUUID();

        // Persist audit metadata ONLY - NEVER persist raw values or samples
        ScanEntity scanEntity = new ScanEntity(
            scanId,
            safeFilename,
            fileType,
            parsed.rowCount(),
            parsed.columnCount(),
            findings.size(),
            LocalDateTime.now(),
            durationMs
        );

        for (ColumnFinding finding : findings) {
            BigDecimal matchRate = BigDecimal.valueOf(finding.matchRate())
                .setScale(2, RoundingMode.HALF_UP);

            ColumnFindingEntity findingEntity = new ColumnFindingEntity(
                UUID.randomUUID(),
                scanEntity,
                finding.columnIndex(),
                finding.type().name(),
                finding.confidence().name(),
                matchRate
            );
            scanEntity.addFinding(findingEntity);
        }

        scanRepository.save(scanEntity);

        // Convert findings to API DTOs (containing masked samples only)
        List<ColumnFindingDto> columnDtos = findings.stream()
            .map(f -> new ColumnFindingDto(
                f.columnIndex(),
                f.columnName(),
                f.type(),
                f.confidence(),
                f.reasons(),
                f.maskedSamples()
            ))
            .toList();

        return new ScanResponseDto(
            scanId,
            safeFilename,
            parsed.rowCount(),
            parsed.columnCount(),
            summary,
            columnDtos,
            ScanResponseDto.DEFAULT_DISCLAIMER
        );
    }

    /**
     * Generates a masked copy of the uploaded file for the selected column indexes.
     */
    public byte[] maskFile(MultipartFile file, List<Integer> selectedColumnIndexes) {
        validateFile(file);

        if (selectedColumnIndexes == null || selectedColumnIndexes.isEmpty()) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "No columns selected for masking");
        }

        String safeFilename = sanitizeFileName(file.getOriginalFilename());
        FileParser parser = parserFactory.getParser(safeFilename);

        ParsedFile parsed;
        try {
            parsed = parser.parse(file.getInputStream());
        } catch (DataMapException e) {
            throw e;
        } catch (Exception e) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "Failed to read file for masking: " + e.getMessage());
        }

        int colCount = parsed.columnCount();
        for (Integer idx : selectedColumnIndexes) {
            if (idx == null || idx < 0 || idx >= colCount) {
                throw new DataMapException(ErrorCode.PARSE_FAILED, "Invalid column index selected: " + idx);
            }
        }

        // Run scanner to resolve column identifier types
        List<ColumnFinding> findings = columnScanner.scan(parsed.headers(), parsed.rows());
        Map<Integer, IdentifierType> columnsToMask = new HashMap<>();
        Set<Integer> genericMaskColumns = new HashSet<>();

        for (ColumnFinding finding : findings) {
            if (selectedColumnIndexes.contains(finding.columnIndex())) {
                columnsToMask.put(finding.columnIndex(), finding.type());
            }
        }

        // For any manually selected column not in findings, infer from header or fall back to conservative generic masking
        for (Integer selectedIdx : selectedColumnIndexes) {
            if (!columnsToMask.containsKey(selectedIdx)) {
                IdentifierType inferred = inferTypeFromHeader(parsed.headers().get(selectedIdx));
                if (inferred != null) {
                    columnsToMask.put(selectedIdx, inferred);
                } else {
                    genericMaskColumns.add(selectedIdx);
                }
            }
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            parser.writeMasked(parsed, bos, columnsToMask, genericMaskColumns);
            return bos.toByteArray();
        } catch (DataMapException e) {
            throw e;
        } catch (Exception e) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "Failed to generate masked file: " + e.getMessage());
        }
    }

    private IdentifierType inferTypeFromHeader(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        for (IdentifierType type : IdentifierType.values()) {
            if (headerMatcher.match(header, type).isMatch()) {
                return type;
            }
        }
        return null;
    }

    /**
     * Strips directory paths, quotes, and control characters from uploaded filenames.
     */
    public static String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "data.csv";
        }
        String baseName = originalFilename.replace('\\', '/');
        int lastSlash = baseName.lastIndexOf('/');
        if (lastSlash >= 0) {
            baseName = baseName.substring(lastSlash + 1);
        }
        String sanitized = baseName.replaceAll("[\"\\r\\n\\t]", "").trim();
        if (sanitized.isEmpty()) {
            return "data.csv";
        }
        return sanitized.length() > 200 ? sanitized.substring(sanitized.length() - 200) : sanitized;
    }

    /**
     * Retrieves scan history containing metadata only.
     */
    @Transactional(readOnly = true)
    public List<ScanHistoryDto> getScanHistory() {
        return scanRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(s -> new ScanHistoryDto(
                s.getId(),
                s.getFileName(),
                s.getFileType(),
                s.getRowCount(),
                s.getColumnCount(),
                s.getFlaggedColumns(),
                s.getCreatedAt(),
                s.getDurationMs()
            ))
            .toList();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DataMapException(ErrorCode.EMPTY_FILE, "Uploaded file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new DataMapException(ErrorCode.FILE_TOO_LARGE, "File exceeds the 10 MB limit.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new DataMapException(ErrorCode.UNSUPPORTED_FILE_TYPE, "Uploaded file name is missing");
        }
        String lower = filename.toLowerCase().trim();
        if (!lower.endsWith(".csv") && !lower.endsWith(".xlsx")) {
            if (lower.endsWith(".xls")) {
                throw new DataMapException(ErrorCode.UNSUPPORTED_FILE_TYPE, "Legacy .xls format is not supported. Please upload .xlsx");
            }
            throw new DataMapException(ErrorCode.UNSUPPORTED_FILE_TYPE, "Unsupported file format. Only .csv and .xlsx files are supported.");
        }
    }

    private String getFileType(String filename) {
        if (filename == null) {
            return "UNKNOWN";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".csv")) {
            return "CSV";
        }
        if (lower.endsWith(".xlsx")) {
            return "XLSX";
        }
        return "UNKNOWN";
    }
}
