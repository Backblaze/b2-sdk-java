/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import com.backblaze.b2.util.B2StringUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Arrays;

import static com.backblaze.b2.json.B2JsonBoundedByteArrayOutputStream.SYSTEM_MAX_CAPACITY;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit test for B2JsonByteArrayOutputStream
 */
public class B2JsonBoundedByteArrayOutputStreamTest {

    @Rule
    public ExpectedException thrown  = ExpectedException.none();

    @Test
    public void testInitialState() {
        final B2JsonBoundedByteArrayOutputStream b2JsonByteArrayOutputStream = new B2JsonBoundedByteArrayOutputStream(128);
        assertArrayEquals(new byte[0], b2JsonByteArrayOutputStream.toByteArray());
        assertEquals(128, b2JsonByteArrayOutputStream.getMaxCapacity());
        assertEquals(0, b2JsonByteArrayOutputStream.getSize());
    }

    @Test
    public void testWriteOneByte() throws IOException {
        final B2JsonBoundedByteArrayOutputStream b2JsonByteArrayOutputStream2Bytes = new B2JsonBoundedByteArrayOutputStream(2048);

        // write a couple of bytes for simple tests
        b2JsonByteArrayOutputStream2Bytes.write('A');
        assertArrayEquals(new byte[]{'A'}, b2JsonByteArrayOutputStream2Bytes.toByteArray());
        assertEquals(1, b2JsonByteArrayOutputStream2Bytes.getSize());

        b2JsonByteArrayOutputStream2Bytes.write('B');
        assertArrayEquals(new byte[] {'A', 'B'}, b2JsonByteArrayOutputStream2Bytes.toByteArray());
        assertEquals(2, b2JsonByteArrayOutputStream2Bytes.getSize());

        // create the actual bytes array to compare to the expected bytes later
        final byte[] actual2048Bytes = new byte[2048];
        final B2JsonBoundedByteArrayOutputStream b2JsonByteArrayOutputStream = new B2JsonBoundedByteArrayOutputStream(2048);

        // write one byte at a time continuously up to the maxCapacity
        for (int i = 0; i < 2048; i++) {
            b2JsonByteArrayOutputStream.write(i);
            actual2048Bytes[i] = (byte)i;
        }

        // OutputStream's array size and content should match actualAllBytes array
        assertArrayEquals(actual2048Bytes, b2JsonByteArrayOutputStream.toByteArray());
        assertEquals(2048, b2JsonByteArrayOutputStream.getSize());

    }

    @Test
    public void testWriteByteArray() throws IOException {
        final B2JsonBoundedByteArrayOutputStream b2JsonByteArrayOutputStream1 = new B2JsonBoundedByteArrayOutputStream(256);

        // write an empty array; output array should be empty
        final byte[] emptyBytes = new byte[0];
        b2JsonByteArrayOutputStream1.write(emptyBytes, 0, emptyBytes.length);
        assertArrayEquals(new byte[0], b2JsonByteArrayOutputStream1.toByteArray());

        final byte[] startBytes = new byte[] {'h', 'e', 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd', ','};
        b2JsonByteArrayOutputStream1.write(startBytes, 0, startBytes.length);
        assertArrayEquals(startBytes, b2JsonByteArrayOutputStream1.toByteArray());
        assertEquals(12, b2JsonByteArrayOutputStream1.getSize());

        // write another array of bytes to the output stream
        final byte[] endBytes = new byte[] {'l', 'e', 't', ' ', 'u', 's', ' ', 'c', 'o', 'd', 'e', '!' };
        b2JsonByteArrayOutputStream1.write(endBytes, 0, endBytes.length);
        assertArrayEquals(appendOneByteArrayToTheOtherByteArray(startBytes, endBytes), b2JsonByteArrayOutputStream1.toByteArray());
        assertEquals(24, b2JsonByteArrayOutputStream1.getSize());

        // catch invalid parameters for byte array input
        try {
            b2JsonByteArrayOutputStream1.write(endBytes, -1, startBytes.length);
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            assertEquals(String.format("offset: %d, len: %d", -1, startBytes.length), indexOutOfBoundsException.getMessage());
        }
        assertEquals(24, b2JsonByteArrayOutputStream1.getSize());

        // create an OutputStream that will take one block of bytes of maxCapacity size
        final B2JsonBoundedByteArrayOutputStream b2JsonByteArrayOutputStream2 = new B2JsonBoundedByteArrayOutputStream(1024);
        final byte[] blockOfBytes = makeByteArraysFilledWithValue(1024, 1);
        b2JsonByteArrayOutputStream2.write(blockOfBytes, 0, 1024);
        assertArrayEquals(blockOfBytes, b2JsonByteArrayOutputStream2.toByteArray());
        assertEquals(1024, b2JsonByteArrayOutputStream2.getSize());
    }

    @Test
    public void testToString() throws IOException {
        final String actualString = "this is a bounded byte array output stream.";
        final B2JsonBoundedByteArrayOutputStream b2JsonBoundedByteArrayOutputStream = new B2JsonBoundedByteArrayOutputStream(128);
        b2JsonBoundedByteArrayOutputStream.write(actualString.getBytes(), 0, actualString.length());
        assertEquals(actualString, b2JsonBoundedByteArrayOutputStream.toString(B2StringUtil.UTF8));
    }

    @Test
    public void testCapacityExpansion() throws IOException {
        // max capacity being 0, cannot write, no expansion at all
        final B2JsonBoundedByteArrayOutputStream cannotWriteToOutputStream = new B2JsonBoundedByteArrayOutputStream(0);
        try {
            cannotWriteToOutputStream.write('1');
        } catch (IOException ioException) {
            assertEquals("Requested array size exceeds maximum limit", ioException.getMessage());
        }
        assertEquals(0, cannotWriteToOutputStream.getSize());

        final B2JsonBoundedByteArrayOutputStream b2JsonByteArrayOutputStream = new B2JsonBoundedByteArrayOutputStream(256);

        // write an array of bytes with length matching the initial capacity
        final byte[] allOnesBytes = makeByteArraysFilledWithValue(64, 1);
        b2JsonByteArrayOutputStream.write(allOnesBytes, 0, 64);
        final byte[] actualBytes = b2JsonByteArrayOutputStream.toByteArray();
        assertArrayEquals(allOnesBytes, actualBytes);

        // trigger one expansion
        final byte[] allZerosBytes = new byte[64];
        b2JsonByteArrayOutputStream.write(allZerosBytes, 0, 64);
        final byte[] expectedExpandedArrayBytes = b2JsonByteArrayOutputStream.toByteArray();
        final byte[] actualExpandedArrayBytes = appendOneByteArrayToTheOtherByteArray(allOnesBytes, allZerosBytes);
        assertArrayEquals(actualExpandedArrayBytes, expectedExpandedArrayBytes);

        // more expansion
        b2JsonByteArrayOutputStream.write(allOnesBytes, 0, 64);
        final byte[] expectedMoreExpandedArrayBytes = b2JsonByteArrayOutputStream.toByteArray();
        final byte[] actualMoreExpandedArrayBytes = appendOneByteArrayToTheOtherByteArray(actualExpandedArrayBytes, allOnesBytes);
        assertArrayEquals(actualMoreExpandedArrayBytes, expectedMoreExpandedArrayBytes);

        // yet more expansion
        b2JsonByteArrayOutputStream.write(allZerosBytes,0, 64);
        final byte[] expectedYetMoreExpandedArrayBytes = b2JsonByteArrayOutputStream.toByteArray();
        final byte[] actualYetMoreExpandedArrayBytes = appendOneByteArrayToTheOtherByteArray(actualMoreExpandedArrayBytes, allZerosBytes);
        assertArrayEquals(actualYetMoreExpandedArrayBytes, expectedYetMoreExpandedArrayBytes);

        // writing a single byte more triggers a threshold exception
        try {
            b2JsonByteArrayOutputStream.write('1');
        } catch (IOException ioException) { // maximum limit imposed by maxCapacity
            assertEquals("Requested array size exceeds maximum limit", ioException.getMessage());
        }

        // output array size should remain the same as the one from last successful write: 256
        assertEquals(actualYetMoreExpandedArrayBytes.length, b2JsonByteArrayOutputStream.getSize());

        // the max capacity is not a power of 2: we can expand and write up to maxCapacity
        final B2JsonBoundedByteArrayOutputStream b2JsonByteArrayOutputStreamToFail = new B2JsonBoundedByteArrayOutputStream(4888);
        for (int i = 0; i < 4888; i++) {
            b2JsonByteArrayOutputStreamToFail.write(i);
        }
        assertEquals(4888, b2JsonByteArrayOutputStreamToFail.getSize());

        // Ideally we'd like to include tests to catch the IOException where needed capacity
        // integer overflows. Such tests passed on local IDEA environment but failed on
        // Travis CI build; Hence these tests are removed here.
}

    @Test
    public void testInvalidMaxCapacityParameters() {
        B2JsonBoundedByteArrayOutputStream b2JsonBoundedByteArrayOutputStream = null;
        try {
            b2JsonBoundedByteArrayOutputStream = new B2JsonBoundedByteArrayOutputStream(-1);
        } catch (IllegalArgumentException illegalArgumentException) {
            assertEquals("maxCapacity must not be negative.", illegalArgumentException.getMessage());
        }
        assertNull(b2JsonBoundedByteArrayOutputStream);

        try {
            b2JsonBoundedByteArrayOutputStream = new B2JsonBoundedByteArrayOutputStream(Integer.MAX_VALUE - 7);
        } catch (IllegalArgumentException illegalArgumentException) {
            assertEquals("maxCapacity must not be bigger than 2147483639", illegalArgumentException.getMessage());
        }
        assertNull(b2JsonBoundedByteArrayOutputStream);
    }

    /* append byte array 2 to the byte array 1 */
    private byte[] appendOneByteArrayToTheOtherByteArray(byte[] byteArray1, byte[] byteArray2) {
        final byte[] appendedByteArray = Arrays.copyOf(byteArray1, byteArray1.length + byteArray2.length);
        System.arraycopy(byteArray2, 0,  appendedByteArray, byteArray1.length, byteArray2.length);
        return  appendedByteArray;
    }

    private byte[] makeByteArraysFilledWithValue(int count, int value) {
        final byte[] largeBuf = new byte[count];
        for (int i = 0; i < count; i++) {
            largeBuf[i] = (byte) value;
        }
        return largeBuf;
    }
}
