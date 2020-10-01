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

import java.io.*;

import static com.backblaze.b2.client.B2TestHelpers.makeBytes;
import static org.junit.Assert.*;

/**
 * This tests a lot of the code in B2ContentOutputStreamWriter.
 * It doesn't test all of the functionality in the base class.
 */
public class B2ContentOutputStreamWriterTest extends B2BaseTest {
    private static final int LEN = 6123;
    private final byte[] bytes = makeBytes(LEN);
    private final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    private final String rightSha1 = B2Sha1.hexSha1OfBytes(bytes);


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static class HelperSimulator implements B2ContentOutputStreamWriter.Helper {
        boolean verifySha1ByRereading = false;

        int numOutputCreations = 0;
        int numInputCreations = 0;
        int numSucceededs = 0;
        int numFaileds = 0;

        // some flags to make bad things happen.
        boolean throwDuringDownload;
        boolean mangleBeforeRereading;

        // the most recent output stream created.
        ByteArrayOutputStream out;

        @Override
        public OutputStream createOutputStream() throws IOException {
            numOutputCreations++;
            assertEquals("called multiple times?", 1, numOutputCreations);
            if (throwDuringDownload) {
                out = new ByteArrayOutputStream() {
                    @Override
                    public synchronized void write(int b) {
                        throw new RuntimeException("testing!");
                    }

                    @Override
                    public synchronized void write(byte[] b, int off, int len) {
                        throw new RuntimeException("testing!");
                    }
                };
            } else {
                out = new ByteArrayOutputStream();
            }
            return out;
        }

        @Override
        public boolean shouldVerifySha1ByRereadingFromDestination() {
            return verifySha1ByRereading;
        }

        @Override
        public InputStream createInputStream() throws IOException {
            assertTrue("we should only be called when re-reading!", verifySha1ByRereading);
            numInputCreations++;

            final byte[] bytes = out.toByteArray();
            if (mangleBeforeRereading) {
                // mangle the bytes so the sha1 doesn't match!
                bytes[0] = (byte) ((bytes[0]+1) % 256);
            }
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void failed() {
            numFaileds++;
        }

        @Override
        public void succeeded() {
            numSucceededs++;
        }

        byte[] getBytes() {
            return out.toByteArray();
        }

        @SuppressWarnings("SameParameterValue")
        void check(int expectedNumOutputCreations,
                   int expectedNumInputCreations,
                   int expectedNumSucceeds,
                   int expectedNumFaileds) {
            assertEquals("numOutputCreations", expectedNumOutputCreations, numOutputCreations);
            assertEquals("numInputCreations", expectedNumInputCreations, numInputCreations);
            assertEquals("numSucceeds", expectedNumSucceeds, numSucceededs);
            assertEquals("numFaileds", expectedNumFaileds, numFaileds);
        }
    };

    @Test
    public void testHappyPath_noVerify() throws B2Exception {
        final HelperSimulator helper = new HelperSimulator();
        helper.verifySha1ByRereading = false;
        final B2ContentOutputStreamWriter writer = B2ContentOutputStreamWriter.builder(helper).build();

        writer.readContent(makeHeaderWithSha1(), in);

        helper.check(
                1,
                0,
                1,
                0
        );
        assertArrayEquals(bytes, helper.getBytes());
    }

    @Test
    public void testHappyPath_verify() throws B2Exception {
        final HelperSimulator helper = new HelperSimulator();
        helper.verifySha1ByRereading = true;

        final B2ContentOutputStreamWriter writer = B2ContentOutputStreamWriter.builder(helper).build();

        writer.readContent(makeHeaderWithSha1(), in);

        helper.check(
                1,
                1,
                1,
                0
        );
        assertArrayEquals(bytes, helper.getBytes());
    }

    @Test
    public void testTroubleDuringDownload() throws B2Exception {
        final HelperSimulator helper = new HelperSimulator();
        helper.throwDuringDownload = true;

        final B2ContentOutputStreamWriter writer = B2ContentOutputStreamWriter.builder(helper).build();

        try {
            writer.readContent(makeHeaderWithSha1(), in);
            fail("should've thrown!");
        } catch (RuntimeException ignored) {
            // we're expecting this!
        }

        helper.check(
                1,
                0,
                0,
                1
        );
    }

    @Test
    public void testSha1MismatchDuringDownload() {
        final HelperSimulator helper = new HelperSimulator();

        // mangle the bytes so the sha1 doesn't match!
        bytes[0] = (byte) ((bytes[0]+1) % 256);

        final B2ContentOutputStreamWriter writer = B2ContentOutputStreamWriter.builder(helper).build();

        try {
            writer.readContent(makeHeaderWithSha1(), in);
            fail("should've thrown!");
        } catch (B2Exception e) {
            assertTrue(e.getMessage().startsWith("sha1 mismatch from network."));
        }

        helper.check(
                1,
                0,
                0,
                1
        );
    }

    @Test
    public void testSha1MismatchDuringRereading() {
        final HelperSimulator helper = new HelperSimulator();
        helper.verifySha1ByRereading = true;
        helper.mangleBeforeRereading = true;

        final B2ContentOutputStreamWriter writer = B2ContentOutputStreamWriter.builder(helper).build();

        try {
            writer.readContent(makeHeaderWithSha1(), in);
            fail("should've thrown!");
        } catch (B2Exception e) {
            assertTrue(e.getMessage().startsWith("sha1 mismatch from destination."));
        }

        helper.check(
                1,
                1,
                0,
                1
        );
    }

    private B2Headers makeHeaderWithSha1() {
        return B2HeadersImpl
                .builder()
                .set(B2Headers.CONTENT_LENGTH, Long.toString(LEN))
                .set(B2Headers.CONTENT_SHA1, rightSha1)
                .build();
    }
}
