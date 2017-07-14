/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.backblaze.b2.client.B2TestHelpers.makeBytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class B2IoUtilsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCopy() throws IOException {
        checkCopy(makeBytes(0));
        checkCopy(makeBytes(100));
        checkCopy(makeBytes((4 * 1024) + 123)); // bigger than the buffer used to copy.
    }

    private void checkCopy(byte[] original) throws IOException {
        final ByteArrayInputStream in = new ByteArrayInputStream(original);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        B2IoUtils.copy(in, out);

        assertArrayEquals(original, out.toByteArray());
    }

    @Test
    public void testCloseQuietly_withoutException() {
        final InputStream thrower = new ByteArrayInputStream(makeBytes(100));
        B2IoUtils.closeQuietly(thrower);
    }

    @Test
    public void testCloseQuietly_withException() {
        final InputStream thrower = new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }

            @Override
            public void close() throws IOException {
                throw new IOException("test!");
            }
        };
        B2IoUtils.closeQuietly(thrower);
    }

    @Test
    public void testReadToEnd() throws IOException {
        final InputStream in = new ByteArrayInputStream(makeBytes(10000));
        B2IoUtils.readToEnd(in);

        // verify we're at the end.
        assertEquals(-1, in.read());

        // close the stream.  there shouldn't be any exceptions.
        in.close();
    }

    @Test
    public void testReadToEnd_hitsException() throws IOException {
        final InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("test");
            }
        };

        thrown.expect(IOException.class);
        thrown.expectMessage("test");
        B2IoUtils.readToEnd(in);
    }

    @Test
    public void test_forCoverage() {
        new B2IoUtils();
    }
}
