/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class B2SdkTest {
    @Test
    public void testName() {
        // this should be quite stable!
        assertEquals("b2-sdk-java", B2Sdk.getName());
    }

    @Test
    public void testVersion() {
        // our versions are three integers separated by periods.
        final Pattern pattern = Pattern.compile("^\\d+[.]\\d+[.]\\d+$");
        final String version = B2Sdk.getVersion();
        assertTrue("version is '" + version + "'", pattern.matcher(version).matches());
    }
}
