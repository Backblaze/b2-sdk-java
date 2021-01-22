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
public class B2JsonBoundedByteArrayOutputStreamTest {

    /**
     * Cannot directly test the case with MAX_ARRAY_SIZE being (Integer.MAX_VALUE - 8)
     * since the real OutOfMemory error would occur without triggering the intended
     * IOException ("Requested array size exceeds maximum limit"). This happens during
     * unit tests and git builds.
     *
     * Instead, create a subclass of B2JsonByteArrayOutputStream with lowered
     * MAX_ARRAY_SIZE (1000) for testing purpose: IOException will then be thrown
     */
    static class B2JsonBoundedByteArrayOutputStreamForTest extends B2JsonBoundedByteArrayOutputStream {
        private static final int MAX_CAPACITY = 1000;

        @Override
        protected int getMaxCapacity() {
            return MAX_CAPACITY;
        }
    }

    @Rule
    public ExpectedException thrown  = ExpectedException.none();

    @Test
    public void testRequestedArraySizeOverTheMaxLimit() throws IOException {
        final B2JsonBoundedByteArrayOutputStreamForTest b2JsonByteArrayOutputStreamForTest = new B2JsonBoundedByteArrayOutputStreamForTest();

        // write an array of 501 bytes to the b2JsonByteArrayOutputStreamForTest first
        b2JsonByteArrayOutputStreamForTest.write(makeByteArrayFilledWith1(501), 0, 501);

        String largeJson = null;
        try {
            // write would fail since the capacity after being doubled would be over (Integer.MAX_VALUE - 8)
            b2JsonByteArrayOutputStreamForTest.write(1);
            largeJson = b2JsonByteArrayOutputStreamForTest.toString();
        } catch (IOException ioException) {
            assertEquals("Requested array size exceeds maximum limit", ioException.getMessage());
        }
        assertNull(largeJson);
    }

    private byte[] makeByteArrayFilledWith1(int count) {
        final byte[] largeBuf = new byte[count];
        for (int i = 0; i < count; i++) {
            largeBuf[i] = (byte)1;
        }
        return largeBuf;
    }
}
