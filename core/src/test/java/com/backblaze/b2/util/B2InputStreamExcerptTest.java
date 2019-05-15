/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class B2InputStreamExcerptTest extends B2BaseTest {
    private static final String DIGITS_STR = "0123456789";
    private static final byte[] DIGITS_BYTES = B2StringUtil.getUtf8Bytes(DIGITS_STR);
    private static final int EOF = -1;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @After
    public void after() {
        // make sure we didn't corrupt the array during the tests.
        assertArrayEquals(B2StringUtil.getUtf8Bytes(DIGITS_STR), DIGITS_BYTES);
    }

    @Test
    public void testNegativeStartThrowsInConstructor() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("start must be non-negative.");

        new B2InputStreamExcerpt(makeDigits(), -1, 1);
    }

    @Test
    public void testEmptyExcerpt_read() throws IOException {
        // length = 0.
        checkRead(excerpt(makeEmpty(), 0, 0), EOF);
        checkRead(excerpt(makeDigits(), 0, 0), EOF);

        // start == underlying length.
        checkRead(excerpt(makeEmpty(), 0, 6), EOF);
        checkRead(excerpt(makeDigits(), 10, 6), EOF);

        // start > underlying length.
        checkRead(excerpt(makeEmpty(), 1, 0), EOF);
        checkRead(excerpt(makeDigits(), 10, 0), EOF);
    }

    @Test
    public void testEmptyExcerpt_readArray() throws IOException {
        // read(byte[]) calls read(byte[],int,int)

        // length = 0.
        checkReadArrayEmpty(excerpt(makeEmpty(), 0, 0));
        checkReadArrayEmpty(excerpt(makeDigits(), 0, 0));

        // start == underlying length.
        checkReadArrayEmpty(excerpt(makeEmpty(), 0, 6));
        checkReadArrayEmpty(excerpt(makeDigits(), 10, 6));

        // start > underlying length.
        checkReadArrayEmpty(excerpt(makeEmpty(), 1, 0));
        checkReadArrayEmpty(excerpt(makeDigits(), 10, 0));
    }

    @Test
    public void testNonEmptyExcerpt_read() throws IOException {
        // a short excerpt, well-within the underlying stream.
        {
            InputStream s = excerpt(makeDigits(), 7, 2);
            checkRead(s, '7');
            checkRead(s, '8');
            checkRead(s, EOF);
        }

        // an excerpt that extends past the underlying stream.
        {
            InputStream s = excerpt(makeDigits(), 8, 3);
            checkRead(s, '8');
            checkRead(s, '9');
            checkRead(s, EOF);
            checkRead(s, EOF);
        }
    }

    @Test
    public void testNonEmptyExcerpt_readArray() throws IOException {
        // a short excerpt, well-within the underlying stream.
        {
            InputStream s = excerpt(makeDigits(), 6, 3);
            checkReadArray(s, new byte[1], 0, 1, 1, new byte[] { '6' });
            checkReadArray(s, new byte[7], 2, 100, 2, new byte[] { 0, 0, '7', '8', 0, 0, 0 });
        }

        // an excerpt that extends past the underlying stream.
        {
            InputStream s = excerpt(makeDigits(), 7, 6);
            checkReadArray(s, new byte[1], 0, 1, 1, new byte[] { '7' });
            checkReadArray(s, new byte[7], 4, 3, 2, new byte[] { 0, 0, 0, 0, '8', '9', 0 });
        }

        // an excerpt that extends past the underlying stream.
        // asking for less data than is in the underlying stream.
        {
            InputStream s = excerpt(makeDigits(), 6, 10);
            checkReadArray(s, new byte[7], 4, 3, 3, new byte[] { 0, 0, 0, 0, '6', '7', '8' });
        }
    }

    @Test
    public void testEmptyExcerpt_skip() throws IOException {
        // length = 0.
        checkSkip(excerpt(makeEmpty(), 0, 0), 1, 0, EOF);
        checkSkip(excerpt(makeDigits(), 0, 0), 1, 0, EOF);

        // start == underlying length.
        checkSkip(excerpt(makeEmpty(), 0, 6), 2, 0, EOF);
        checkSkip(excerpt(makeDigits(), 10, 6), 3, 0, EOF);

        // start > underlying length.
        checkSkip(excerpt(makeEmpty(), 1, 0), 1, 0, EOF);
        checkSkip(excerpt(makeDigits(), 10, 0), 1, 0, EOF);
    }

    @Test
    public void testNonEmptyExcerpt_skip() throws IOException {
        // skip a little within the excerpt.
        checkSkip(excerpt(makeDigits(), 1, 8), 1, 1, '2');

        // skip up to the end of the excerpt.
        checkSkip(excerpt(makeDigits(), 2, 6), 6, 6, EOF);

        // try to skip past the end of the excerpt.
        checkSkip(excerpt(makeDigits(), 2, 6), 7, 6, EOF);
    }

    @Test
    public void testEmptyExcerpt_available() throws IOException {
        // length = 0.
        assertEquals(0, excerpt(makeEmpty(), 0, 0).available());
        assertEquals(0, excerpt(makeDigits(), 0, 0).available());

        // start == underlying length.
        assertEquals(0, excerpt(makeEmpty(), 0, 6).available());
        assertEquals(0, excerpt(makeDigits(), 10, 6).available());

        // start > underlying length.
        assertEquals(0, excerpt(makeEmpty(), 1, 0).available());;
        assertEquals(0, excerpt(makeDigits(), 10, 0).available());;
    }

    @Test
    public void testNonEmptyExcerpt_available() throws IOException {
        // a short excerpt, well-within the underlying stream.
        assertEquals(3, excerpt(makeDigits(), 6, 3).available());

        // an excerpt that extends past the underlying stream.
        assertEquals(4, excerpt(makeDigits(), 6, 6).available());
    }

    @Test
    public void testClose() throws IOException {
        final InputStream in = mock(InputStream.class);
        excerpt(in, 0, 10).close();

        verify(in, times(1)).close();
    }

    private void checkSkip(InputStream in,
                           int requestedSkip,
                           int expectedSkip,
                           int nextByte) throws IOException {
        assertEquals(expectedSkip, in.skip(requestedSkip));
        assertEquals(nextByte, in.read());
    }

    private void checkReadArray(InputStream s,
                                byte[] readInto,
                                int offset,
                                int length,
                                int expectedReadByteCount,
                                byte[] expected) throws IOException {
        assertEquals(expectedReadByteCount, s.read(readInto, offset, length));
        assertArrayEquals(expected, readInto);
    }

    private void checkReadArrayEmpty(InputStream in) throws IOException {
        final byte[] bytes = new byte[] { 1, 2, 3, 4, 5, 6 };

        assertEquals(EOF, in.read(bytes));

        // check no bytes were changed
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, bytes);
    }


    private void checkRead(InputStream in, int expectedValue) throws IOException {
        final int actual = in.read();
        assertEquals(expectedValue, actual);
    }

    private InputStream excerpt(InputStream in, long start, long length) {
        return new B2InputStreamExcerpt(in, start, length);
    }

    private InputStream makeEmpty() {
        return new ByteArrayInputStream(new byte[0]);
    }

    private InputStream makeDigits() {
        return new ByteArrayInputStream(DIGITS_BYTES);
    }
}
