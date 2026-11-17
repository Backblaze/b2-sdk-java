/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class B2UtilFindingsTest extends B2BaseTest {
    private static byte[] seq(int n) {
        final byte[] b = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = (byte) i;
        }
        return b;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            out.write(c);
        }
        return out.toByteArray();
    }

    /** Finding C21: after skip(n) the excerpt must still return all of its remaining bytes. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC21_skipDoesNotShortenExcerpt() throws IOException {
        final B2InputStreamExcerpt excerpt = new B2InputStreamExcerpt(new ByteArrayInputStream(seq(10)), 0, 10);
        assertEquals(3, excerpt.skip(3));
        final byte[] rest = readAll(excerpt);
        assertEquals("bytes 3..9 expected after skipping 3 of 10", 7, rest.length);
        assertEquals(3, rest[0]);
        assertEquals(9, rest[6]);
    }

    /** Finding C20: an excerpt longer than 2 GiB must still be readable (remaining-bytes count must not overflow). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC20_excerptLongerThan2GiBCanBeRead() throws IOException {
        final long threeGiB = 3L * 1024 * 1024 * 1024;
        final B2InputStreamExcerpt excerpt = new B2InputStreamExcerpt(new ByteArrayInputStream(seq(100)), 0, threeGiB);
        final byte[] buf = new byte[16];
        final int n = excerpt.read(buf, 0, buf.length);
        assertEquals(16, n);
        assertEquals(15, buf[15]);
    }

    /** Finding C29: a range number too large for a long must give null (as documented), not throw. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC29_parseOverflowingNumberReturnsNull() {
        try {
            assertNull(B2ByteRange.parse("bytes=99999999999999999999-"));
            assertNull(B2ByteRange.parse("bytes=0-99999999999999999999"));
        } catch (NumberFormatException e) {
            fail("parse() threw instead of returning null: " + e);
        }
    }

    /**
     * Findings C23 and C49: a space in a file name must be encoded so that it cannot be mistaken for a literal
     * plus in a URL path (RFC 3986 path encoding is %20).  NOTE: whether the B2 server itself accepts "+" as a
     * space needs a live request; this test asserts the URL-standard behaviour.
     */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC23_C49_spaceInFileNameIsPercentTwentyNotPlus() {
        final String encoded = B2StringUtil.percentEncode("a b+c");
        assertEquals("a%20b%2Bc", encoded);
    }
}
