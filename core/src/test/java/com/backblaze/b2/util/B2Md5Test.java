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

public class B2Md5Test extends B2BaseTest {

    @Test
    public void testMd5() {
        final String expectedMd5 = "5eb63bbbe01eeed093cb22bb8f5acdc3";
        byte[] bytes = B2StringUtil.getUtf8Bytes("hello world");

        assertEquals(expectedMd5, B2Md5.hexMd5OfBytes(bytes));
        assertEquals(expectedMd5.length(), B2Md5.HEX_MD5_SIZE);
    }
}
