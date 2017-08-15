/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2SdkTest {
    @Test
    public void testName() {
        // this should be quite stable!
        assertEquals("b2-sdk-java", B2Sdk.getName());
    }

    @Test
    public void testVersion() {
        // we'll have to update this for each release.  :)
        assertEquals("0.0.5", B2Sdk.getVersion());
    }
}
