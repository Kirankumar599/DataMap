package com.datamap.engine.scoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SamplerTest {

    @Test
    @DisplayName("Spreads samples across the entire file rather than only first 200 rows")
    void testSystematicSampling() {
        Sampler sampler = new Sampler(200);

        // Generate 1000 rows
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            rows.add("Value_" + i);
        }

        List<String> sampled = sampler.sample(rows);
        assertThat(sampled).hasSize(200);

        // First sample should be near 0, last sample should be near the end of 1000
        assertThat(sampled.get(0)).isEqualTo("Value_0");
        assertThat(sampled.get(199)).isEqualTo("Value_995");
    }

    @Test
    @DisplayName("Filters out null, blank, and empty values before sampling")
    void testFiltersBlanks() {
        Sampler sampler = new Sampler(200);

        List<String> rows = List.of(
            "Val1", "", "   ", "Val2", "Val3"
        );
        List<String> sampled = sampler.sample(rows);

        assertThat(sampled).containsExactly("Val1", "Val2", "Val3");
    }

    @Test
    @DisplayName("Returns all non-empty values when count is less than max sample size")
    void testUnderSampleSize() {
        Sampler sampler = new Sampler(200);
        List<String> rows = List.of("A", "B", "C");

        List<String> sampled = sampler.sample(rows);
        assertThat(sampled).containsExactly("A", "B", "C");
    }
}
