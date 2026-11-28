package com.datamap.controller;

import com.datamap.dto.ScanHistoryDto;
import com.datamap.dto.ScanResponseDto;
import com.datamap.dto.ScanSummaryDto;
import com.datamap.enums.ErrorCode;
import com.datamap.exception.DataMapException;
import com.datamap.exception.GlobalExceptionHandler;
import com.datamap.service.ScanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScanControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ScanService scanService;

    @InjectMocks
    private ScanController scanController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(scanController, new HealthController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("GET /api/health returns ok status")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("POST /api/scans returns 200 with scan findings for valid upload")
    void testScanSuccess() throws Exception {
        UUID scanId = UUID.randomUUID();
        ScanResponseDto mockResponse = new ScanResponseDto(
            scanId,
            "sample.csv",
            10,
            2,
            new ScanSummaryDto(1, 1, 0, 0),
            Collections.emptyList(),
            ScanResponseDto.DEFAULT_DISCLAIMER
        );

        when(scanService.scanFile(any())).thenReturn(mockResponse);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sample.csv",
            "text/csv",
            "Name,Phone\nTest,9876543210".getBytes()
        );

        mockMvc.perform(multipart("/api/scans").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scanId").value(scanId.toString()))
            .andExpect(jsonPath("$.fileName").value("sample.csv"))
            .andExpect(jsonPath("$.summary.flaggedColumns").value(1));
    }

    @Test
    @DisplayName("POST /api/scans returns 400 when file is empty")
    void testScanEmptyFile() throws Exception {
        when(scanService.scanFile(any()))
            .thenThrow(new DataMapException(ErrorCode.EMPTY_FILE, "Uploaded file is empty"));

        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/scans").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("EMPTY_FILE"))
            .andExpect(jsonPath("$.message").value("Uploaded file is empty"));
    }

    @Test
    @DisplayName("POST /api/mask returns downloadable masked copy")
    void testMaskDownload() throws Exception {
        byte[] sampleMasked = "Name,Phone\nTest,XXXXXX3210\n".getBytes();
        when(scanService.maskFile(any(), anyList())).thenReturn(sampleMasked);

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sample.csv",
            "text/csv",
            "Name,Phone\nTest,9876543210".getBytes()
        );

        mockMvc.perform(multipart("/api/mask")
                .file(file)
                .param("columns", "[1]"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"masked_sample.csv\""))
            .andExpect(content().bytes(sampleMasked));
    }

    @Test
    @DisplayName("GET /api/scans returns audit metadata history")
    void testGetHistory() throws Exception {
        UUID id = UUID.randomUUID();
        ScanHistoryDto historyItem = new ScanHistoryDto(
            id,
            "sample.csv",
            "CSV",
            10,
            3,
            1,
            LocalDateTime.now(),
            45
        );
        when(scanService.getScanHistory()).thenReturn(List.of(historyItem));

        mockMvc.perform(get("/api/scans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].fileName").value("sample.csv"))
            .andExpect(jsonPath("$[0].rowCount").value(10));
    }

    @Test
    @DisplayName("POST /api/scans returns 400 FILE_TOO_LARGE when upload exceeds 10MB")
    void testScanFileTooLarge() throws Exception {
        when(scanService.scanFile(any()))
            .thenThrow(new MaxUploadSizeExceededException(10L * 1024 * 1024));

        MockMultipartFile file = new MockMultipartFile("file", "big.csv", "text/csv", "a,b\n1,2".getBytes());

        mockMvc.perform(multipart("/api/scans").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("FILE_TOO_LARGE"));
    }
}
