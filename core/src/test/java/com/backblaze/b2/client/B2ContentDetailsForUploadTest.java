/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2IoUtils;
import com.backblaze.b2.util.B2StringUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.assertEquals;

public class B2ContentDetailsForUploadTest extends B2BaseTest {
    private static final String CONTENTS = "Hello, World!";
    private static final byte[] CONTENTS_BYTES = B2StringUtil.getUtf8Bytes(CONTENTS);
    private static final String SHA1 = "0a0a9f2a6772942557ab5355d76af442f8f65e01";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // simple class to subclass for tests below.
    private static class DummySource implements B2ContentSource {
        @Override
        public long getContentLength() throws IOException {
            return 0;
        }

        @Override
        public String getSha1OrNull() throws IOException {
            return null;
        }

        @Override
        public Long getSrcLastModifiedMillisOrNull() throws IOException {
            return null;
        }

        @Override
        public InputStream createInputStream() throws IOException {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    @Test
    public void testWithSha1() throws B2Exception, IOException {
        final B2ContentSource source = B2ByteArrayContentSource
                .builder(CONTENTS_BYTES)
                .setSha1OrNull(SHA1)
                .build();
        final B2ContentDetailsForUpload details = new B2ContentDetailsForUpload(source);

        assertEquals(CONTENTS_BYTES.length, details.getContentLength());
        assertEquals(SHA1, details.getContentSha1HeaderValue());
        checkStreamContents(CONTENTS, details.getInputStream());
    }

    @Test
    public void testWithoutSha1() throws B2Exception, IOException {
        final B2ContentSource source = B2ByteArrayContentSource
                .builder(CONTENTS_BYTES)
                .build();
        final B2ContentDetailsForUpload details = new B2ContentDetailsForUpload(source);

        assertEquals(CONTENTS_BYTES.length + SHA1.length(), details.getContentLength());
        assertEquals(B2Headers.HEX_DIGITS_AT_END, details.getContentSha1HeaderValue());
        checkStreamContents(CONTENTS + SHA1, details.getInputStream());
    }

    @Test
    public void testGetContentLengthThrows() throws B2Exception {
        thrown.expect(B2LocalException.class);
        thrown.expectMessage("failed to get contentLength from source: java.io.IOException: testing");

        new B2ContentDetailsForUpload(new DummySource() {
            @Override
            public long getContentLength() throws IOException {
                throw new IOException("testing");
            }
        });
    }

    @Test
    public void testGetSha1OrNullThrows() throws B2Exception {
        thrown.expect(B2LocalException.class);
        thrown.expectMessage("trouble getting sha1 from source: java.io.IOException: testing");

        new B2ContentDetailsForUpload(new DummySource() {
            @Override
            public String getSha1OrNull() throws IOException {
                throw new IOException("testing");
            }
        });
    }

    @Test
    public void testCreateInputStreamThrows() throws B2Exception {
        thrown.expect(B2LocalException.class);
        thrown.expectMessage("failed to create inputStream from source: java.io.IOException: testing");

        new B2ContentDetailsForUpload(new DummySource() {
            @Override
            public InputStream createInputStream() throws IOException {
                throw new IOException("testing");
            }
        });
    }

    private void checkStreamContents(String expectedContents,
                                     InputStream inputStream) throws IOException {
        // i'm doing these checks in "string-space" so they're easier to look at.
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        B2IoUtils.copy(inputStream, outputStream);

        final String actualContents = outputStream.toString(B2StringUtil.UTF8);
        assertEquals(expectedContents, actualContents);
    }
}
