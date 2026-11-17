/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentHandlers;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.contentSources.B2HeadersImpl;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Sha1;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class B2ContentFileWriterFindingsTest extends B2BaseTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final String OLD = "previous good contents";

    private static B2Headers headers(int len, String sha1OrNull) {
        final B2HeadersImpl.Builder b = B2HeadersImpl.builder().set(B2Headers.CONTENT_LENGTH, "" + len);
        if (sha1OrNull != null) {
            b.set(B2Headers.CONTENT_SHA1, sha1OrNull);
        }
        return b.build();
    }

    private File existingFile() throws IOException {
        final File f = folder.newFile("target.bin");
        Files.write(f.toPath(), OLD.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    /** Finding C4: a download whose checksum is wrong must not destroy the older file at the destination. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC4_checksumMismatchLeavesPreviousFileIntact() throws Exception {
        final File f = existingFile();
        final byte[] data = "new bytes".getBytes(StandardCharsets.UTF_8);
        final String wrongSha1 = B2Sha1.hexSha1OfBytes("something else".getBytes(StandardCharsets.UTF_8));

        try {
            B2ContentFileWriter.builder(f).build().readContent(headers(data.length, wrongSha1), new ByteArrayInputStream(data));
            fail("expected sha1 mismatch");
        } catch (B2Exception expected) {
            // expected
        }
        assertEquals(OLD, new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
    }

    /** Finding C4: a dropped connection must not leave a truncated file (or destroy the older one). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC4_droppedConnectionLeavesPreviousFileIntact() throws Exception {
        final File f = existingFile();
        final InputStream dropping = new InputStream() {
            private int n;
            @Override
            public int read() throws IOException {
                if (n++ < 5) {
                    return 'x';
                }
                throw new IOException("connection reset");
            }
        };
        try {
            B2ContentFileWriter.builder(f).build().readContent(headers(100, null), dropping);
            fail("expected failure");
        } catch (B2Exception expected) {
            // expected
        }
        assertEquals(OLD, new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
    }
}
