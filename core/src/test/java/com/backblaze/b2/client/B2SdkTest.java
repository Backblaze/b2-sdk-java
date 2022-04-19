/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class B2SdkTest extends B2BaseTest {
    // our versions are three integers separated by periods, with an
    // optional "-PRIVATE+<number>" at the end.  see semver.org
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+[.]\\d+[.]\\d+(-[a-zA-Z0-9\\+]+)?$");

    @Test
    public void testName() {
        // this should be quite stable!
        assertEquals("b2-sdk-java", B2Sdk.getName());
    }

    @Test
    public void testIsValidVersionHelper() {
        assertTrue(isValidVersion("0.0.6"));
        assertTrue(isValidVersion("1.11.22"));
        assertTrue(isValidVersion("1.11.22-rc"));

        assertTrue(!isValidVersion(""));
        assertTrue(!isValidVersion("1"));
        assertTrue(!isValidVersion("1.11"));
        assertTrue(!isValidVersion("1.11.22-"));
        assertTrue(!isValidVersion("1.11.22-!"));
    }

    @Test
    public void testVersion() {
        final String version = B2Sdk.getVersion();
        assertTrue("version is '" + version + "'", isValidVersion(version));
    }

    private static boolean isValidVersion(String version) {
        return VERSION_PATTERN.matcher(version).matches();
    }
}
