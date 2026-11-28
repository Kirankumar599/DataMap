package com.datamap.config;

import com.datamap.engine.ColumnScanner;
import com.datamap.engine.DefaultColumnScanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for CORS and Engine bean integration.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${datamap.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }

    @Bean
    public ColumnScanner columnScanner() {
        return new DefaultColumnScanner();
    }
}
