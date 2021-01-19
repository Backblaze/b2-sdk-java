/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;

/**
 * Unit test for B2JsonByteArrayOutputStream
 */
public class B2JsonByteArrayOutputStreamTest {

    @Rule
    public ExpectedException thrown  = ExpectedException.none();

    @Test
    public void testRequestedArraySizeOverTheMaxLimit() throws IOException {
        final B2JsonByteArrayOutputStream b2JsonByteArrayOutputStream = new B2JsonByteArrayOutputStream();

        // allocate a byte array of half Integer.MAX_VALUE: write should work
        final byte[] largeBuf = new byte[Integer.MAX_VALUE/2];
        b2JsonByteArrayOutputStream.write(largeBuf, 0, largeBuf.length);

        String largeJson = null;
        try {
            // write would fail since the capacity after being doubled would be over (Integer.MAX_VALUE - 8)
            b2JsonByteArrayOutputStream.write('a');
            largeJson = b2JsonByteArrayOutputStream.toString();
        } catch (IOException ioException) {
            assertEquals("Requested array size exceeds maximum limit", ioException.getMessage());
        }
        assertNull(largeJson);
    }
}
