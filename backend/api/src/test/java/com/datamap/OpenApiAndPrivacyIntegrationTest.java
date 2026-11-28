package com.datamap;

import com.datamap.entity.ScanEntity;
import com.datamap.repository.ScanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class OpenApiAndPrivacyIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ScanRepository scanRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("OpenAPI v3 spec is live and documents core endpoints")
    void testOpenApiEndpointLive() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("DataMap API"))
            .andExpect(jsonPath("$.paths['/api/scans']").exists())
            .andExpect(jsonPath("$.paths['/api/mask']").exists())
            .andExpect(jsonPath("$.paths['/api/health']").exists());
    }

    @Test
    @DisplayName("Full scan -> DB metadata check -> mask flow never persists raw cell values")
    void testEndToEndPrivacyBoundary() throws Exception {
        scanRepository.deleteAll();

        String rawPhone = "9876543210";
        String rawPan = "ABCPK1234F";
        String rawEmail = "ravi.kumar@gmail.com";
        String csv = "Name,Phone,PAN,Email\nRavi," + rawPhone + "," + rawPan + "," + rawEmail;

        MockMultipartFile file = new MockMultipartFile(
            "file", "college_test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/scans").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.flaggedColumns").value(3))
            .andExpect(jsonPath("$.disclaimer").isNotEmpty());

        List<ScanEntity> saved = scanRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getFileName()).isEqualTo("college_test.csv");
        assertThat(saved.get(0).getFlaggedColumns()).isEqualTo(3);

        mockMvc.perform(multipart("/api/mask").file(file).param("columns", "[1,2,3]"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"masked_college_test.csv\""));
    }
}
