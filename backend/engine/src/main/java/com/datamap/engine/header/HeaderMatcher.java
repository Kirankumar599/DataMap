package com.datamap.engine.header;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.HeaderMatchResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes column headers and matches them against identifier keyword dictionaries
 * using exact token matching and fuzzy Levenshtein distance.
 */
public class HeaderMatcher {

    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.80;

    private final Map<IdentifierType, List<String>> keywordsMap;
    private final double similarityThreshold;

    public HeaderMatcher() {
        this(DEFAULT_SIMILARITY_THRESHOLD);
    }

    public HeaderMatcher(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
        this.keywordsMap = new EnumMap<>(IdentifierType.class);

        keywordsMap.put(IdentifierType.AADHAAR, List.of(
            "aadhaar", "aadhar", "adhar", "uid", "uidai", "aadhaar card", "aadhar card"
        ));
        keywordsMap.put(IdentifierType.PAN, List.of(
            "pan", "pan number", "permanent account number", "pancard", "pan card", "pan no"
        ));
        keywordsMap.put(IdentifierType.PHONE, List.of(
            "phone", "mobile", "contact", "cell", "ph no", "telephone", "mobile number",
            "phone number", "contact number", "cell number", "ph"
        ));
        keywordsMap.put(IdentifierType.EMAIL, List.of(
            "email", "e-mail", "mail address", "email address", "e mail", "mail", "user email"
        ));
    }

    /**
     * Normalizes a header string:
     * - Converts to lowercase
     * - Replaces punctuation and non-alphanumeric chars (except spaces) with space
     * - Collapses multiple spaces and trims
     */
    public String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String lower = header.toLowerCase();
        // Replace underscores, hyphens, and other punctuation with spaces
        String replaced = lower.replaceAll("[^a-z0-9\\s]", " ");
        // Collapse multiple whitespace characters into a single space
        return replaced.replaceAll("\\s+", " ").trim();
    }

    /**
     * Evaluates a header for a specific identifier type.
     */
    public HeaderMatchResult match(String rawHeader, IdentifierType type) {
        if (rawHeader == null) {
            return HeaderMatchResult.noMatch(type);
        }

        String normalized = normalizeHeader(rawHeader);
        if (normalized.isEmpty()) {
            return HeaderMatchResult.noMatch(type);
        }

        List<String> keywords = keywordsMap.getOrDefault(type, Collections.emptyList());
        List<String> tokens = Arrays.asList(normalized.split(" "));

        // 1. Direct exact match with full header or full phrase match
        for (String kw : keywords) {
            if (normalized.equals(kw)) {
                return new HeaderMatchResult(type, true, false, kw, 1.0);
            }
        }

        // 2. Token-level exact match (e.g. "student_aadhaar_number" contains "aadhaar")
        for (String kw : keywords) {
            if (tokens.contains(kw) || normalized.contains(kw)) {
                return new HeaderMatchResult(type, true, false, kw, 1.0);
            }
        }

        // 3. Fuzzy matching: check whole string or individual tokens against keywords
        double bestSimilarity = 0.0;
        String bestKeyword = null;

        for (String kw : keywords) {
            // Compare whole normalized header if short
            double wholeSim = calculateSimilarity(normalized, kw);
            if (wholeSim > bestSimilarity) {
                bestSimilarity = wholeSim;
                bestKeyword = kw;
            }

            // Compare each token
            for (String token : tokens) {
                if (token.length() >= 3) {
                    double tokenSim = calculateSimilarity(token, kw);
                    if (tokenSim > bestSimilarity) {
                        bestSimilarity = tokenSim;
                        bestKeyword = kw;
                    }
                }
            }
        }

        if (bestSimilarity >= similarityThreshold && bestKeyword != null) {
            return new HeaderMatchResult(type, true, true, bestKeyword, bestSimilarity);
        }

        return HeaderMatchResult.noMatch(type);
    }

    /**
     * Calculates Levenshtein similarity between two strings: 1.0 - (distance / max(len1, len2)).
     */
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }
        int distance = computeLevenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Computes classical Levenshtein edit distance.
     */
    public int computeLevenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int[] prev = new int[len2 + 1];
        int[] curr = new int[len2 + 1];

        for (int j = 0; j <= len2; j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            curr[0] = i;
            char c1 = s1.charAt(i - 1);
            for (int j = 1; j <= len2; j++) {
                char c2 = s2.charAt(j - 1);
                int cost = (c1 == c2) ? 0 : 1;
                curr[j] = Math.min(
                    Math.min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                );
            }
            System.arraycopy(curr, 0, prev, 0, len2 + 1);
        }

        return prev[len2];
    }
}
