/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentHandlers;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2ConnectionBrokenException;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.util.B2IoUtils;
import com.backblaze.b2.util.B2Preconditions;
import com.backblaze.b2.util.B2Sha1;
import com.backblaze.b2.util.B2Sha1InputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.backblaze.b2.util.B2IoUtils.closeQuietly;

/**
 * B2ContentWriter is a base class for ContentHandlers which writes the content
 * to a stream.  Different subclasses support different types of output
 * streams.
 *
 * If possible, this class will verify the SHA1 of the downloaded content.
 * It is possible when:
 *   * the content is NOT a partial (ie "range") request.
 *   * the response include either a "X-Bz-Content-Sha1" or
 *     "X-Bz-Info-large_file_sha1" header.  (Note that if the
 *     sha1 header's value starts with "unverified:", the remainder
 *     of the value will be used.)
 *
 * If possible (see above), this class will always verify the SHA1 of the
 * content as it's downloaded.
 *
 * If possible (see above) and requested, this class will also re-read the
 * content from its destination to verify that it has been stored to the destination
 * with no errors.
 *
 * THREAD-SAFETY: this object is NOT thread-safe on its own.  users should
 *                only use it from one thread at a time and do proper locking
 *                to ensure changes are visible to other threads as needed.
 */
public abstract class B2ContentWriter implements B2ContentSink {
    private static final int EOF = -1;
    private static final int DEFAULT_COPY_BUFFER_SIZE = 4 * 1024;
    private final boolean verifySha1ByRereadingFromDestination;
    private B2Headers headers; // null until readContent is called.

    B2ContentWriter(boolean verifySha1ByRereadingFromDestination) {
        this.verifySha1ByRereadingFromDestination = verifySha1ByRereadingFromDestination;
    }

    // for tests.
    boolean getVerifySha1ByRereadingFromDestination() {
        return verifySha1ByRereadingFromDestination;
    }

    @Override
    public void readContent(B2Headers responseHeaders,
                            InputStream rawIn) throws B2Exception {
        // grab the headers for later.
        this.headers = responseHeaders;

        // this is null if we can't check the sha1 for this input stream.
        final String expectedSha1OrNull = getSha1ToCheckOrNull(this.headers);

        // we'll read from 'in'.
        final InputStream in;
        if (expectedSha1OrNull != null) {
            in =  new B2Sha1InputStream(rawIn);
        } else {
            in = rawIn;
        }

        // save the content!
        OutputStream out = null;
        boolean failed = true;
        try {

            // copy to the destination.
            out = createDestinationOutputStreamOrThrow();
            copy(in, out);
            closeOrThrow(out);

            // if possible, verify that the right data was read from the network.
            maybeCheckSha1("from network", expectedSha1OrNull, in);

            // if possible and requested, verify that the right data made it to the destination.
            maybeVerifySha1FromDestination(expectedSha1OrNull);

            failed = false;
            succeeded();
        } finally {
            closeQuietly(out);

            if (failed) {
                failed();
            }
        }
    }

    /**
     * If expectedSha1OrNull isn't null, this casts the input stream to
     * a B2Sha1InputStream and throws if the actual sha1 doesn't match the
     * expectedSha1OrNull.
     *
     * @param expectedSha1OrNull null or the expected sha1.
     * @param in if expectedSha1OrNull is non-null,
     */
    private void maybeCheckSha1(String when,
                                String expectedSha1OrNull,
                                InputStream in) throws B2LocalException {
        if (expectedSha1OrNull == null) {
            return;
        }

        B2Preconditions.checkArgument(in instanceof B2Sha1InputStream);
        final String actualSha1 = ((B2Sha1InputStream) in).hexDigest();
        if (!B2Sha1.equalHexSha1s(expectedSha1OrNull, actualSha1)) {
            throw new B2LocalException("mismatch", "sha1 mismatch " + when + ".  expected " + expectedSha1OrNull + ", but got " + actualSha1);
        }
    }

    private void maybeVerifySha1FromDestination(String expectedSha1OrNull) throws B2Exception {
        if (expectedSha1OrNull == null) {
            return;
        }
        if (!verifySha1ByRereadingFromDestination) {
            return;
        }

        try (InputStream fromDest = createDestinationInputStreamOrThrow();
            final B2Sha1InputStream sha1er = new B2Sha1InputStream(fromDest)) {
            B2IoUtils.readToEnd(sha1er);

            maybeCheckSha1("from destination", expectedSha1OrNull, sha1er);
        } catch (IOException e) {
            throw new B2LocalException("write_failed", "failed to verify the sha1: " + e.getMessage(), e);
        }
    }


    /*forTests*/ static String getSha1ToCheckOrNull(B2Headers headers) {
        if (headers.hasContentRange()) {
            return null;
        }

        // sha1 == "none" for large files, and so we don't want to return "none".
        // instead, we want to fall through and check for the optional largeFileSha1.
        final String sha1 = headers.getContentSha1EvenIfUnverifiedOrNull();
        if (sha1 != null && !sha1.equals("none")) {
            return sha1;
        }

        final String largeSha1 = headers.getLargeFileSha1OrNull();
        if (largeSha1 != null) {
            return largeSha1;
        }

        return null;
    }

    // a helper for createDestinationOutputStream() which makes it easy to throw
    // a more meaningful exception if there's trouble.
    private OutputStream createDestinationOutputStreamOrThrow() throws B2Exception {
        try {
            return createDestinationOutputStream();
        } catch (IOException e) {
            throw new B2LocalException("write_failed", "couldn't open destination output stream to write it: " + e.getMessage(), e);
        }
    }

    // a helper for close() which makes it easy to throw
    // a more meaningful exception if there's trouble.
    private void closeOrThrow(Closeable closeable) throws B2Exception {
        try {
            closeable.close();
        } catch (IOException e) {
            throw new B2LocalException("write_failed", "couldn't close destination output stream: " + e.getMessage(), e);
        }
    }

    // a helper for createDestinationInputStream() which makes it easy to throw
    // a more meaningful exception if there's trouble.
    private InputStream createDestinationInputStreamOrThrow() throws B2Exception {
        try {
            return createDestinationInputStream();
        } catch (IOException e) {
            throw new B2LocalException("write_failed", "couldn't open destination input stream to check it: " + e.getMessage(), e);
        }
    }

    /**
     * @return an outputStream to write to the destination. calling this is allowed to
     *         destroy the existing output (if any) and make it impossible for
     *         a stream created by createDestinationInputStream() to be able to
     *         read the data, if any.
     * @apiNote this may be called multiple times.
     * @throws IOException if there's any trouble
     */
    protected abstract OutputStream createDestinationOutputStream() throws IOException;

    /**
     * @return a new inputStream to read from the destination.
     *         this might not contain what you think it should if readContents
     *         hasn't completed successfully.
     * @apiNote this may be called multiple times.
     * @throws IOException if there's any trouble
     */
    protected abstract InputStream createDestinationInputStream() throws IOException;

    /**
     * Called when the download succeeded and, to the extent we're able to verify it, the SHA1 matched.
     *
     * Called at most once for each time that createDestinationOutputStream() is called.
     */
    protected void succeeded() {
    }

    /**
     * Called after we've created an output stream, but then the download failed for some reason.
     * That reason may include errors with the download, or, to the extent we can verify it, an
     * error with the SHA1 of the written content.
     *
     * Called at most once for each time that createDestinationOutputStream() is called.  Called before
     * an attempt to retry the download (if any).
     */
    protected void failed() {
    }

    /**
     * @return the headers from the server, if any.
     *         returns null until readContent is called.
     *         may return non-null even if there's an exception
     *         during readContent().
     */
    public B2Headers getHeadersOrNull() {
        return headers;
    }

    private static void copy(InputStream in,
                             OutputStream out) throws B2Exception {
        copy(in, out, new byte[DEFAULT_COPY_BUFFER_SIZE]);
    }

    // inspired by IOUtils.
    private static void copy(InputStream input,
                             OutputStream output,
                             byte[] buffer)
            throws B2Exception {
        while (true) {
            int nRead;

            try {
                nRead = input.read(buffer);
                if (nRead == EOF) {
                    break;
                }
            } catch (IOException e) {
                throw new B2ConnectionBrokenException("read_failed", null, "read failed: " + e.getMessage(), e);
            }

            try {
                output.write(buffer, 0, nRead);
            } catch (IOException e) {
                throw new B2LocalException("write_failed", "write failed: " + e.getMessage(), e);
            }
        }
    }

}
