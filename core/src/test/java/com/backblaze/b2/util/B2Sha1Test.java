/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class B2Sha1Test {

    @Test
    public void testSha1() throws IOException {
        final String expectedSha1 = "2aae6c35c94fcfb415dbe95f408b9ce91ee846ed";
        byte[] bytes = B2StringUtil.getUtf8Bytes("hello world");

        assertEquals(expectedSha1, B2Sha1.hexSha1OfBytes(bytes));
        assertEquals(expectedSha1, B2Sha1.hexSha1OfInputStream(new ByteArrayInputStream(bytes)));

        final String upperSha1 = expectedSha1.toUpperCase();
        assertTrue(!expectedSha1.equals(upperSha1));
        assertTrue(B2Sha1.equalHexSha1s(expectedSha1, upperSha1));
    }
}
