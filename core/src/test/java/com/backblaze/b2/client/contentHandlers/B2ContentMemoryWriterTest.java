/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentHandlers;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.contentSources.B2HeadersImpl;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Sha1;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;

import static com.backblaze.b2.client.B2TestHelpers.makeBytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test *exercises* some of the code in B2ContentFileWriter.
 * The code in the base class is tested elsewhere.  Also, to avoid
 * actually writing to disk, it doesn't really write to disk.  Bummer, huh?
 * Hopefully there's more exercising of it elsewhere.
 */
public class B2ContentMemoryWriterTest extends B2BaseTest {
    private static final int LEN = 6123;
    private final byte[] bytes = makeBytes(LEN);
    private final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    private final String rightSha1 = B2Sha1.hexSha1OfBytes(bytes);


    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testBuilder() {
        assertTrue(B2ContentMemoryWriter
                .build()
                .getVerifySha1ByRereadingFromDestination());
        assertTrue(!B2ContentMemoryWriter
                .builder()
                .setVerifySha1ByRereadingFromDestination(false)
                .build()
                .getVerifySha1ByRereadingFromDestination());
    }

    @Test
    public void testContentTooLong() throws B2Exception {

        final long tooLong = Integer.MAX_VALUE + 1L;
        final B2ContentMemoryWriter writer = B2ContentMemoryWriter.build();

        thrown.expect(B2Exception.class);
        thrown.expectMessage("contentLength is too big for this B2ContentMemoryWriter.  (2147483648 > 2147483647)");
        writer.readContent(makeHeaderWithSha1(tooLong), in);
    }

    @Test
    public void testReadContent() throws B2Exception {
        final ByteArrayInputStream in = new ByteArrayInputStream(makeBytes(LEN));

        final B2ContentMemoryWriter writer = B2ContentMemoryWriter.build();
        assertEquals(0, writer.getBytes().length); // none yet.

        writer.readContent(makeHeaderWithSha1(LEN), in);

        assertArrayEquals(makeBytes(LEN), writer.getBytes());
    }

    private B2Headers makeHeaderWithSha1(long contentLength) {
        return B2HeadersImpl
                .builder()
                .set(B2Headers.CONTENT_LENGTH, "" + contentLength)
                .set(B2Headers.CONTENT_SHA1, rightSha1)
                .build();
    }
}
