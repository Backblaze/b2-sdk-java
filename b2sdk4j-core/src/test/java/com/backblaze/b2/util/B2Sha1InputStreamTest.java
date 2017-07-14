/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class B2Sha1InputStreamTest {

    @Test
    public void testAllReadMethods() throws IOException {
        byte [] data = makeData(100);
        B2Sha1InputStream in = new B2Sha1InputStream(new ByteArrayInputStream(data));
        assertEquals(0, in.read());
        assertEquals(2, in.read(new byte[2]));
        assertEquals(4, in.skip(4));
        assertEquals(8, in.skip(8));
        assertEquals(10, in.skip(10));
        assertEquals(75, in.read(new byte[200], 100, 100));
        assertEquals(-1, in.read());
        assertEquals(-1, in.read(new byte [2]));
        assertEquals(-1, in.read(new byte [100], 0, 100));
        assertEquals(0, in.skip(10));

        assertArrayEquals(
                B2Sha1.binarySha1OfBytes(data),
                in.digest()
        );
    }

    @SuppressWarnings("SameParameterValue")
    private byte [] makeData(int byteCount) {
        byte [] result = new byte[byteCount];
        for (int i = 0; i < byteCount; i++) {
            result[i] = (byte) i;
        }
        return result;
    }
}

