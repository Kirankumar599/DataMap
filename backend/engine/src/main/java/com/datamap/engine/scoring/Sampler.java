package com.datamap.engine.scoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Samples non-empty cell values evenly across a file column to avoid inspecting every row
 * while ensuring broad representation across large files.
 */
public class Sampler {

    public static final int DEFAULT_SAMPLE_SIZE = 200;

    private final int maxSampleSize;

    public Sampler() {
        this(DEFAULT_SAMPLE_SIZE);
    }

    public Sampler(int maxSampleSize) {
        this.maxSampleSize = maxSampleSize;
    }

    /**
     * Extracts non-empty values from a column and samples up to maxSampleSize values
     * systematically spread across the available non-empty rows.
     *
     * @param allColumnValues all row values for a specific column
     * @return sampled non-empty values
     */
    public List<String> sample(List<String> allColumnValues) {
        if (allColumnValues == null || allColumnValues.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter out null, empty, and blank values
        List<String> nonEmpty = new ArrayList<>();
        for (String val : allColumnValues) {
            if (val != null && !val.trim().isEmpty()) {
                nonEmpty.add(val.trim());
            }
        }

        int totalNonEmpty = nonEmpty.size();
        if (totalNonEmpty <= maxSampleSize) {
            return nonEmpty;
        }

        // Systematic sampling evenly spread across all available non-empty rows
        List<String> sampled = new ArrayList<>(maxSampleSize);
        double step = (double) totalNonEmpty / maxSampleSize;
        for (int i = 0; i < maxSampleSize; i++) {
            int index = (int) Math.floor(i * step);
            if (index < totalNonEmpty) {
                sampled.add(nonEmpty.get(index));
            }
        }

        return sampled;
    }
}
