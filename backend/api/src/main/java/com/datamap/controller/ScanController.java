package com.datamap.controller;

import com.datamap.dto.ScanHistoryDto;
import com.datamap.dto.ScanResponseDto;
import com.datamap.enums.ErrorCode;
import com.datamap.exception.DataMapException;
import com.datamap.service.ScanService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller handling scan uploads, masking requests, and scan history.
 */
@RestController
@RequestMapping("/api")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    /**
     * POST /api/scans
     * Uploads and scans a CSV or XLSX file for Indian sensitive identifiers.
     */
    @PostMapping(value = "/scans", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ScanResponseDto> scanFile(
        @RequestParam("file") MultipartFile file
    ) {
        ScanResponseDto response = scanService.scanFile(file);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/mask
     * Generates a downloadable copy of the file with selected column indexes masked.
     */
    @PostMapping(value = "/mask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> maskFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("columns") String columnsParam
    ) {
        List<Integer> selectedColumns = parseColumnsParam(columnsParam);
        byte[] maskedBytes = scanService.maskFile(file, selectedColumns);

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "data.csv";
        }
        String downloadName = "masked_" + originalName;

        MediaType mediaType = originalName.toLowerCase().endsWith(".xlsx")
            ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            : MediaType.parseMediaType("text/csv");

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
            .body(maskedBytes);
    }

    /**
     * GET /api/scans
     * Retrieves audit history containing scan metadata only.
     */
    @GetMapping("/scans")
    public ResponseEntity<List<ScanHistoryDto>> getScanHistory() {
        List<ScanHistoryDto> history = scanService.getScanHistory();
        return ResponseEntity.ok(history);
    }

    private List<Integer> parseColumnsParam(String columnsParam) {
        if (columnsParam == null || columnsParam.isBlank()) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "No columns selected for masking");
        }
        String clean = columnsParam.replaceAll("[\\[\\]\\s]", "");
        if (clean.isEmpty()) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "No columns selected for masking");
        }

        List<Integer> list = new ArrayList<>();
        try {
            String[] parts = clean.split(",");
            for (String p : parts) {
                if (!p.isBlank()) {
                    list.add(Integer.parseInt(p.trim()));
                }
            }
        } catch (Exception e) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "Invalid columns parameter format. Expected JSON array or comma-separated integers.");
        }

        if (list.isEmpty()) {
            throw new DataMapException(ErrorCode.PARSE_FAILED, "No columns selected for masking");
        }
        return list;
    }
}
