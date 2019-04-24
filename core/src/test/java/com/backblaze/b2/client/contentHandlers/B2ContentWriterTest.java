/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentHandlers;

import com.backblaze.b2.client.B2TestHelpers;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.contentSources.B2HeadersImpl;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2ByteRange;
import com.backblaze.b2.util.B2Sha1;
import com.backblaze.b2.util.B2StringUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.backblaze.b2.client.B2TestHelpers.makeBytes;
import static com.backblaze.b2.client.contentHandlers.B2ContentWriter.getSha1ToCheckOrNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test *exercises* some of the code in B2ContentFileWriter.
 * The code in the base class is tested elsewhere.  Also, to avoid
 * actually writing to disk, it doesn't really write to disk.  Bummer, huh?
 * Hopefully there's more exercising of it elsewhere.
 */
public class B2ContentWriterTest extends B2BaseTest {
    private static final int LEN = 6123;

    private final byte[] bytes = makeBytes(LEN);
    private final String rightSha1 = B2Sha1.hexSha1OfBytes(bytes);
    private final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    private final Writer writer = new Writer(true);



    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // this class is similar to the B2ContentMemoryWriter, but a little simpler
    // and it tracks extra things for the tests.

    private static class Writer extends B2ContentWriter {
        private ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        // true iff createDestinationInputStream was called.
        private boolean inputStreamCreated;
        private boolean throwInCreateDestinationInputStream;
        private boolean throwInGetDestinationOutputStream;
        private boolean throwInOutputStreamClose;
        private boolean throwWhileReadingInputStream;
        private boolean throwWhileWritingOutputStream;

        private Writer(boolean verifySha1ByRereadingFromDestination) {
            super(verifySha1ByRereadingFromDestination);
        }

        @Override
        protected OutputStream createDestinationOutputStream() throws IOException {
            if (throwInGetDestinationOutputStream) {
                throw new IOException("test throw from createDestinationOutputStream");
            }
            if (throwInOutputStreamClose) {
                return new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                    }

                    @Override
                    public void close() throws IOException {
                        throw new IOException("test throwing in output stream close");
                    }
                };
            }
            if (throwWhileWritingOutputStream) {
                return new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        throw new IOException("test throwing in output stream write");
                    }
                };
            }
            return byteStream;
        }

        @Override
        protected InputStream createDestinationInputStream() throws IOException {
            inputStreamCreated = true;
            if (throwInCreateDestinationInputStream) {
                throw new IOException("test throw from createDestinationInputStream");
            }
            if (throwWhileReadingInputStream) {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("test throwing in destination input stream read");
                    }
                };
            }
            return new ByteArrayInputStream(getBytes());
        }

        byte[] getBytes() {
            return byteStream.toByteArray();
        }

        boolean wasInputStreamCreated() {
            return inputStreamCreated;
        }

        void addToDestination(byte[] toAdd) throws IOException {
            byteStream.write(toAdd);
        }

        void setThrowInCreateDestinationInputStream() {
            throwInCreateDestinationInputStream = true;
        }
        void setThrowInGetDestinationOutputStream() {
            throwInGetDestinationOutputStream = true;
        }

        void setThrowInOutputStreamClose() {
            throwInOutputStreamClose = true;
        }

        public void setThrowWhileReadingInputStream() {
            throwWhileReadingInputStream = true;
        }

        public void setThrowWhileWritingOutputStream() {
            throwWhileWritingOutputStream = true;
        }
    }

    @Test
    public void testGetSha1ToCheckOrNull() {
        final String a = B2TestHelpers.makeSha1(1);
        final String b = B2TestHelpers.makeSha1(2);
        final String unverifiedA = "unverified:" + a;

        // range == null, so it's *possible* to check the sha1.
        assertEquals(null, getSha1ToCheckOrNull(makeHeaders(null, null, null)));
        assertEquals(a, getSha1ToCheckOrNull(makeHeaders(null, null, a)));
        assertEquals(a, getSha1ToCheckOrNull(makeHeaders(null, a, null)));
        assertEquals(a, getSha1ToCheckOrNull(makeHeaders(null, unverifiedA, null)));
        assertEquals(a, getSha1ToCheckOrNull(makeHeaders(null, a, b)));  // we prefer the 'small file' sha1 today.
        assertEquals(b, getSha1ToCheckOrNull(makeHeaders(null, "none", b)));  // we need to ignore the "none" that the server sends for large files.
        assertEquals(null, getSha1ToCheckOrNull(makeHeaders(null, "none", null)));  // we need to ignore the "none" that the server sends for large files.

        // range != null, so it's NOT to check the sha1.
        final B2ByteRange contentRange = B2ByteRange.startAt(2);
        assertEquals(null, getSha1ToCheckOrNull(makeHeaders(contentRange, null, null)));
        assertEquals(null, getSha1ToCheckOrNull(makeHeaders(contentRange, null, a)));
        assertEquals(null, getSha1ToCheckOrNull(makeHeaders(contentRange, a, null)));
        assertEquals(null, getSha1ToCheckOrNull(makeHeaders(contentRange, unverifiedA, null)));
        assertEquals(null, getSha1ToCheckOrNull(makeHeaders(contentRange, a, b)));
    }

    @Test
    public void testContentIsCompletelyReadAndHeadersMatch() throws B2Exception {
        final B2Headers headers = makeHeaders(LEN);
        writer.readContent(headers, in);

        assertArrayEquals(makeBytes(LEN), writer.getBytes());
        assertEquals(headers, writer.getHeadersOrNull());
        assertTrue(!sha1WasVerifiedFromDestination(writer));  // cuz no sha1 provided
    }

    @Test
    public void testNoSha1ChecksPerformed() throws B2Exception {
        // having a range prevents the sha1 checks from happening, even though we'd like them to.
        writer.readContent(makeHeadersWithRange(LEN), in);

        assertArrayEquals(makeBytes(LEN), writer.getBytes());
        assertTrue(!sha1WasVerifiedFromDestination(writer));  // cuz range in headers
    }

    @Test
    public void testBothSha1ChecksPass_withSmallFileSha1Header() throws B2Exception {
        writer.readContent(makeHeadersWithLenAndSha1(bytes.length, rightSha1), in);

        assertArrayEquals(makeBytes(LEN), writer.getBytes());
        assertTrue(sha1WasVerifiedFromDestination(writer));
    }

    @Test
    public void testBothSha1ChecksPass_withLargeFileSha1Header() throws B2Exception {
        writer.readContent(makeHeadersWithLenAndLargeFileSha1(bytes.length, rightSha1), in);

        assertArrayEquals(makeBytes(LEN), writer.getBytes());
        assertTrue(sha1WasVerifiedFromDestination(writer));
    }

    @Test
    public void testSha1CheckFailsDuringDownload() throws B2Exception {
        final String wrongSha1 = B2Sha1.hexSha1OfBytes(makeBytes(LEN+1));

        thrown.expect(B2Exception.class);
        thrown.expectMessage("sha1 mismatch from network.  expected " + wrongSha1 + ", but got " + rightSha1);
        writer.readContent(makeHeadersWithLenAndSha1(bytes.length, wrongSha1), in);
    }

    @Test
    public void testSha1CheckFailsWhileRereadingFromDestination() throws B2Exception, IOException {
        writer.addToDestination(B2StringUtil.getUtf8Bytes("ab")); // this will mess up the checksum we get later!

        thrown.expect(B2Exception.class);
        thrown.expectMessage("sha1 mismatch from destination.  expected " + rightSha1 + ", but got 52386c157cc658dc5794f01e40bf55c752f1ff79");
        writer.readContent(makeHeadersWithLenAndLargeFileSha1(bytes.length, rightSha1), in);
    }

    @Test
    public void testRereadingFromDestinationIsntPerformedWhenToldNotTo() throws B2Exception, IOException {
        writer.addToDestination(B2StringUtil.getUtf8Bytes("ab")); // this will mess up the checksum from the destination

        // this writer doesn't double check its work, so it won't notice the mismatch in the destination.
        final Writer lackadaisicalWriter = new Writer(false);
        assertTrue(!lackadaisicalWriter.getVerifySha1ByRereadingFromDestination());

        lackadaisicalWriter.readContent(makeHeadersWithLenAndLargeFileSha1(bytes.length, rightSha1), in);

        assertTrue(!sha1WasVerifiedFromDestination(lackadaisicalWriter));
    }

    @Test
    public void testThrowInCreateDestinationInputStream() throws B2Exception {
        writer.setThrowInCreateDestinationInputStream();

        thrown.expect(B2Exception.class);
        thrown.expectMessage("couldn't open destination input stream to check it: test throw from createDestinationInputStream");
        writer.readContent(makeHeadersWithLenAndSha1(bytes.length, rightSha1), in);
    }

    @Test
    public void testThrowInGetDestinationOutputStream() throws B2Exception {
        writer.setThrowInGetDestinationOutputStream();

        thrown.expect(B2Exception.class);
        thrown.expectMessage("couldn't open destination output stream to write it: test throw from createDestinationOutputStream");
        writer.readContent(makeHeadersWithLenAndSha1(bytes.length, rightSha1), in);
    }

    @Test
    public void testThrowInOutputStreamClose() throws B2Exception {
        writer.setThrowInOutputStreamClose();

        thrown.expect(B2Exception.class);
        thrown.expectMessage("couldn't close destination output stream: test throwing in output stream close");
        writer.readContent(makeHeadersWithLenAndSha1(bytes.length, rightSha1), in);
    }

    @Test
    public void testThrowWhileWritingOutputStream() throws B2Exception {
        writer.setThrowWhileWritingOutputStream();

        thrown.expect(B2Exception.class);
        thrown.expectMessage("write failed: test throwing in output stream write");
        writer.readContent(makeHeadersWithLenAndSha1(bytes.length, rightSha1), in);
    }

    @Test
    public void testThrowWhileReadingTheInputWeAreHandling() throws B2Exception {
        final InputStream throwingIn = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("throwing while reading input to handler");
            }
        };

        thrown.expect(B2Exception.class);
        thrown.expectMessage("read failed: throwing while reading input to handler");
        writer.readContent(makeHeaders(LEN), throwingIn);
    }

    @Test
    public void testThrowsWhileReadingInputStream() throws B2Exception {
        writer.setThrowWhileReadingInputStream();

        thrown.expect(B2Exception.class);
        thrown.expectMessage("failed to verify the sha1: test throwing in destination input stream read");
        writer.readContent(makeHeadersWithLenAndSha1(bytes.length, rightSha1), in);
    }

    private B2Headers makeHeaders(B2ByteRange contentRange, String sha1, String largeFileSha1) {
        final B2HeadersImpl.Builder builder = B2HeadersImpl.builder();
        if (contentRange != null) {
            builder.set(B2Headers.CONTENT_RANGE, contentRange.toString());
        }
        if (sha1 != null) {
            builder.set(B2Headers.CONTENT_SHA1, sha1);
        }
        if (largeFileSha1 != null) {
            builder.set(B2Headers.LARGE_FILE_SHA1, largeFileSha1);
        }
        return builder.build();
    }

    private B2Headers makeHeaders(int contentLength) {
        return B2HeadersImpl
                .builder()
                .set(B2Headers.CONTENT_LENGTH, "" + contentLength)
                .build();
    }
    private B2Headers makeHeadersWithLenAndSha1(int contentLength, String sha1) {
        return B2HeadersImpl
                .builder()
                .set(B2Headers.CONTENT_LENGTH, "" + contentLength)
                .set(B2Headers.CONTENT_SHA1, sha1)
                .build();
    }
    private B2Headers makeHeadersWithLenAndLargeFileSha1(int contentLength, String sha1) {
        return B2HeadersImpl
                .builder()
                .set(B2Headers.CONTENT_LENGTH, "" + contentLength)
                .set(B2Headers.LARGE_FILE_SHA1, sha1)
                .build();
    }
    private B2Headers makeHeadersWithRange(int contentLength) {
        return B2HeadersImpl
                .builder()
                .set(B2Headers.CONTENT_LENGTH, "" + contentLength)
                .set(B2Headers.RANGE, B2ByteRange.startAt(1).toString())
                .build();
    }

    private boolean sha1WasVerifiedFromDestination(Writer writer) {
        // this is our proxy for whether the fromDestination check happened.
        return writer.wasInputStreamCreated();
    }
}
