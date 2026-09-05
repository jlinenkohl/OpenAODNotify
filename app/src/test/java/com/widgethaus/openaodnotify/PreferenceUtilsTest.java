package com.widgethaus.openaodnotify;

import org.junit.Test;
import static org.junit.Assert.*;

public class PreferenceUtilsTest {

    @Test
    public void testColorValidation() {
        assertTrue(PreferenceUtils.isValidColor("0066ff"));
        assertTrue(PreferenceUtils.isValidColor("#0066ff"));
        assertTrue(PreferenceUtils.isValidColor("FFFFFF"));
        assertFalse(PreferenceUtils.isValidColor("blue"));
        assertFalse(PreferenceUtils.isValidColor("FF00"));
        assertFalse(PreferenceUtils.isValidColor("G00000")); // Invalid hex
    }
}
