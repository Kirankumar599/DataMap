package com.datamap.engine.header;

import com.datamap.engine.enums.IdentifierType;
import com.datamap.engine.model.HeaderMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderMatcherTest {

    private HeaderMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new HeaderMatcher();
    }

    @Test
    @DisplayName("Normalizes headers by removing punctuation and multiple spaces")
    void testHeaderNormalization() {
        assertThat(matcher.normalizeHeader("Employee_Aadhaar-No."))
            .isEqualTo("employee aadhaar no");
        assertThat(matcher.normalizeHeader("  USER   EMAIL   ADDRESS  "))
            .isEqualTo("user email address");
        assertThat(matcher.normalizeHeader("PAN#Number"))
            .isEqualTo("pan number");
        assertThat(matcher.normalizeHeader(null)).isEqualTo("");
    }

    @Test
    @DisplayName("Matches exact keywords across all identifier types")
    void testExactKeywordMatching() {
        HeaderMatchResult aadhaar = matcher.match("Aadhaar Number", IdentifierType.AADHAAR);
        assertThat(aadhaar.isMatch()).isTrue();
        assertThat(aadhaar.isFuzzy()).isFalse();

        HeaderMatchResult pan = matcher.match("PAN Card", IdentifierType.PAN);
        assertThat(pan.isMatch()).isTrue();
        assertThat(pan.isFuzzy()).isFalse();

        HeaderMatchResult phone = matcher.match("Contact Mobile No", IdentifierType.PHONE);
        assertThat(phone.isMatch()).isTrue();
        assertThat(phone.isFuzzy()).isFalse();

        HeaderMatchResult email = matcher.match("Email Address", IdentifierType.EMAIL);
        assertThat(email.isMatch()).isTrue();
        assertThat(email.isFuzzy()).isFalse();
    }

    @Test
    @DisplayName("Matches fuzzy header variations and common misspellings above threshold")
    void testFuzzyMatching() {
        // "aadhar" or "adhar" are directly in keywords, but test slight misspelling like "aadharr"
        HeaderMatchResult aadhaarFuzzy = matcher.match("aadharr", IdentifierType.AADHAAR);
        assertThat(aadhaarFuzzy.isMatch()).isTrue();
        assertThat(aadhaarFuzzy.similarity()).isGreaterThanOrEqualTo(0.85);

        // "moble" vs "mobile" (similarity 5/6 = ~0.83)
        HeaderMatchResult phoneFuzzy = matcher.match("moble", IdentifierType.PHONE);
        assertThat(phoneFuzzy.isMatch()).isTrue();
        assertThat(phoneFuzzy.similarity()).isGreaterThanOrEqualTo(0.80);
    }

    @Test
    @DisplayName("Rejects unrelated headers")
    void testUnrelatedHeaders() {
        HeaderMatchResult result = matcher.match("Salary Amount", IdentifierType.AADHAAR);
        assertThat(result.isMatch()).isFalse();

        HeaderMatchResult result2 = matcher.match("Invoice ID", IdentifierType.EMAIL);
        assertThat(result2.isMatch()).isFalse();

        // Brief hard cases: misleading headers and substring traps
        assertThat(matcher.match("Col_B", IdentifierType.AADHAAR).isMatch()).isFalse();
        assertThat(matcher.match("ID", IdentifierType.AADHAAR).isMatch()).isFalse();
        assertThat(matcher.match("Remarks", IdentifierType.EMAIL).isMatch()).isFalse();
        assertThat(matcher.match("Company", IdentifierType.PAN).isMatch()).isFalse();
        assertThat(matcher.match("Participant", IdentifierType.PAN).isMatch()).isFalse();
        assertThat(matcher.match("Paragraph", IdentifierType.PHONE).isMatch()).isFalse();
        assertThat(matcher.match("Student_Guid", IdentifierType.AADHAAR).isMatch()).isFalse();
    }

    @Test
    @DisplayName("Matches brief header variations: Adhar, Aadhar No, Mob no, E-mail")
    void testBriefHeaderVariations() {
        assertThat(matcher.match("Adhar", IdentifierType.AADHAAR).isMatch()).isTrue();
        assertThat(matcher.match("Aadhar No", IdentifierType.AADHAAR).isMatch()).isTrue();
        assertThat(matcher.match("Mob no", IdentifierType.PHONE).isMatch()).isTrue();
        assertThat(matcher.match("E-mail", IdentifierType.EMAIL).isMatch()).isTrue();
    }
}
