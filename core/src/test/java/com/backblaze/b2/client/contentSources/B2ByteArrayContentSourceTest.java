/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentSources;

import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2StringUtil;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.backblaze.b2.client.B2TestHelpers.SAMPLE_SHA1;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class B2ByteArrayContentSourceTest extends B2BaseTest {
    private static final byte[] sourceBytes = B2StringUtil.getUtf8Bytes("Hello, World!");
    private static final Long SRC_LAST_MOD_MILLIS = 123456L;

    @Test
    public void testSimple() throws IOException {
        final B2ContentSource contentSource = B2ByteArrayContentSource.build(sourceBytes);
        assertNull(contentSource.getSha1OrNull());
        assertNull(contentSource.getSrcLastModifiedMillisOrNull());
        assertEquals(sourceBytes.length, contentSource.getContentLength());

        final byte[] readBytes = readAllBytes(contentSource::createInputStream);
        assertArrayEquals(sourceBytes, readBytes);
    }

    @Test
    public void testOptionalAttributes() throws IOException {

        final B2ContentSource contentSource = B2ByteArrayContentSource
                .builder(sourceBytes)
                .setSha1OrNull(SAMPLE_SHA1)
                .setSrcLastModifiedMillisOrNull(SRC_LAST_MOD_MILLIS)
                .build();
        assertEquals(SAMPLE_SHA1, contentSource.getSha1OrNull());
        assertEquals(SRC_LAST_MOD_MILLIS, contentSource.getSrcLastModifiedMillisOrNull());

        final byte[] readBytes = readAllBytes(contentSource::createInputStream);
        assertArrayEquals(sourceBytes, readBytes);
    }

    private interface InputStreamFactory {
        InputStream create() throws IOException;
    }

    private static byte[] readAllBytes(InputStreamFactory inputStreamFactory) throws IOException {
        try (final InputStream input = inputStreamFactory.create();
             final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int b;
            while ((b = input.read()) != -1) {
                out.write(b);
            }
            return out.toByteArray();
        }
    }
}
