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

    @Test
    public void testTimeoutClamping() {
        assertEquals(5, PreferenceUtils.clampTimeout(5));
        assertEquals(1, PreferenceUtils.clampTimeout(0));
        assertEquals(1, PreferenceUtils.clampTimeout(-10));
        assertEquals(720, PreferenceUtils.clampTimeout(1000));
        assertEquals(720, PreferenceUtils.clampTimeout(720));
    }
}
