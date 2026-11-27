package com.datamap.engine.model;

import com.datamap.engine.enums.IdentifierType;

/**
 * Result of header keyword and fuzzy matching.
 */
public record HeaderMatchResult(
    IdentifierType type,
    boolean isMatch,
    boolean isFuzzy,
    String matchedKeyword,
    double similarity
) {
    public static HeaderMatchResult noMatch(IdentifierType type) {
        return new HeaderMatchResult(type, false, false, null, 0.0);
    }
}
