package com.datamap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI specification metadata for DataMap REST endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dataMapOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("DataMap API")
                .version("1.0.0")
                .description(
                    "Privacy-first spreadsheet scanner for Indian personal identifiers "
                    + "(Aadhaar, PAN, Indian Mobile, Email). Files stay strictly in transient memory; "
                    + "only scan metadata is persisted."
                ));
    }
}
