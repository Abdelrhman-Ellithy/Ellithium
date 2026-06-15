package Ellithium.core.ai;

import Ellithium.core.ai.locators.LocatorTechniques;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Behaviour spec for {@link LocatorTechniques#looksDynamic}.
 *
 * Pattern logic (in order):
 *   1. null / blank → false
 *   2. all-digit string (trim) → true
 *   3. DYNAMIC regex find → true:
 *      \\d{4,}                      four or more consecutive digits
 *      [0-9a-fA-F]{8}-[0-9a-fA-F]{4}  UUID-like prefix (8hex-4hex)
 *      _\\d+$                       trailing underscore-then-digits (framework index)
 *      :id/                         Android resource-id namespace separator
 *      [0-9a-f]{16,}               16+ lowercase hex chars (hash / session token)
 */
public class LocatorTechniquesTest {

    @Test
    public void looksDynamic_null_isFalse() {
        Assert.assertFalse(LocatorTechniques.looksDynamic(null));
    }

    @Test
    public void looksDynamic_empty_isFalse() {
        Assert.assertFalse(LocatorTechniques.looksDynamic(""));
    }

    @Test
    public void looksDynamic_blank_isFalse() {
        Assert.assertFalse(LocatorTechniques.looksDynamic("   "));
    }

    @Test
    public void looksDynamic_allDigits_isTrue() {
        // Any string whose trimmed form consists only of digits is dynamic
        Assert.assertTrue(LocatorTechniques.looksDynamic("1"));
        Assert.assertTrue(LocatorTechniques.looksDynamic("42"));
        Assert.assertTrue(LocatorTechniques.looksDynamic("123"));
        Assert.assertTrue(LocatorTechniques.looksDynamic("99999"));
    }

    @Test
    public void looksDynamic_stableAlphabeticIds_isFalse() {
        Assert.assertFalse(LocatorTechniques.looksDynamic("login-btn"));
        Assert.assertFalse(LocatorTechniques.looksDynamic("submitForm"));
        Assert.assertFalse(LocatorTechniques.looksDynamic("search-input"));
        Assert.assertFalse(LocatorTechniques.looksDynamic("user_name"));
    }

    @Test
    public void looksDynamic_fewDigits_notAllDigit_isFalse() {
        // 3-digit run is not ≥4, and the whole string is not all-digit
        Assert.assertFalse(LocatorTechniques.looksDynamic("abc123def"));
        Assert.assertFalse(LocatorTechniques.looksDynamic("item-23"));
        Assert.assertFalse(LocatorTechniques.looksDynamic("a1b2c3"));
    }

    @Test
    public void looksDynamic_fourPlusDigitRun_isTrue() {
        // \\d{4,} matches any run of ≥4 consecutive digit characters
        Assert.assertTrue(LocatorTechniques.looksDynamic("abc1234def"));
        Assert.assertTrue(LocatorTechniques.looksDynamic("react-root-12345"));
        Assert.assertTrue(LocatorTechniques.looksDynamic("elem-1234567"));
    }

    @Test
    public void looksDynamic_trailingUnderscoreDigit_isTrue() {
        // _\\d+$ fires when a string ends with an underscore followed by digits
        Assert.assertTrue(LocatorTechniques.looksDynamic("user_1"));
        Assert.assertTrue(LocatorTechniques.looksDynamic("btn_123"));
        Assert.assertTrue(LocatorTechniques.looksDynamic("row_9999"));
    }

    @Test
    public void looksDynamic_underscoreDigit_notAtEnd_isFalse() {
        // _\\d+$ requires digits at the very end — a suffix after the digits breaks the match
        // Also no 4-digit run exists in "btn_123_suffix"
        Assert.assertFalse(LocatorTechniques.looksDynamic("btn_123_suffix"));
        Assert.assertFalse(LocatorTechniques.looksDynamic("data_testid"));
        Assert.assertFalse(LocatorTechniques.looksDynamic("my_button_primary"));
    }

    @Test
    public void looksDynamic_uuidLikePrefix_isTrue() {
        // [0-9a-fA-F]{8}-[0-9a-fA-F]{4} matches UUID segment 1 + segment 2
        Assert.assertTrue(LocatorTechniques.looksDynamic("550e8400-e29b-41d4"));
        Assert.assertTrue(LocatorTechniques.looksDynamic("AABBCCDD-EEFF-1122"));
    }

    @Test
    public void looksDynamic_appiumResourceIdPrefix_isTrue() {
        // :id/ is the Android resource-id namespace separator
        Assert.assertTrue(LocatorTechniques.looksDynamic("com.example:id/submit_btn"));
        Assert.assertTrue(LocatorTechniques.looksDynamic("android:id/home"));
    }

    @Test
    public void looksDynamic_sixteenLowercaseHexChars_isTrue() {
        // [0-9a-f]{16,} matches hash-like tokens (session ids, CSRFs, etc.)
        Assert.assertTrue(LocatorTechniques.looksDynamic("deadbeef12345678"));
        Assert.assertTrue(LocatorTechniques.looksDynamic("cafebabe0000feed1"));
    }

    @Test
    public void looksDynamic_eightDigitRunInUppercaseHex_isTrue() {
        // Uppercase hex bypasses [0-9a-f]{16,} but the 8-digit run still triggers \\d{4,}
        Assert.assertTrue(LocatorTechniques.looksDynamic("AABBCCDD12345678"));
    }

    @Test
    public void looksDynamic_stableMixedCase_isFalse() {
        Assert.assertFalse(LocatorTechniques.looksDynamic("loginButton"));
        Assert.assertFalse(LocatorTechniques.looksDynamic("searchInput"));
        Assert.assertFalse(LocatorTechniques.looksDynamic("my-stable-class"));
    }

    @Test
    public void looksDynamic_hyphenSeparatedShortNumbers_isFalse() {
        // Hyphen-separated short numbers don't reach the 4-digit threshold
        Assert.assertFalse(LocatorTechniques.looksDynamic("v1-2-3"));
        Assert.assertFalse(LocatorTechniques.looksDynamic("col-md-6"));
    }

    @Test
    public void looksDynamic_longDigitRunEmbedded_isTrue() {
        // Digit run embedded in a real-world dynamic class/id pattern
        Assert.assertTrue(LocatorTechniques.looksDynamic("ng-scope-1234567890"));
        Assert.assertTrue(LocatorTechniques.looksDynamic("vue-component-8765"));
    }
}
