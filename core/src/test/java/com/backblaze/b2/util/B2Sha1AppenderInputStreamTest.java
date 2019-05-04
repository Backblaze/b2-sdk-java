/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class B2Sha1AppenderInputStreamTest extends B2BaseTest {
    private static final String CONTENTS = "Hello, World!";
    private static final String SHA1 = "0a0a9f2a6772942557ab5355d76af442f8f65e01";

    private final InputStream original = new ByteArrayInputStream(B2StringUtil.getUtf8Bytes(CONTENTS));
    private final InputStream withSha1 = B2Sha1AppenderInputStream.create(original);
    private final byte[] dest = new byte[CONTENTS.length() + SHA1.length()]; // relies on all being ascii so #bytes = #characters.


    @Test
    public void testMultipleReads() throws IOException {
        int i=0;

        // read some at start of array
        i += withSha1.read(dest, i, 7);

        // read individual bytes
        for (int j=0; j < 6; j++) {
            dest[i] = (byte) withSha1.read();
            i++;
        }

        // read some more, with offset into an array.
        i += withSha1.read(dest, i, dest.length-i);

        // check!
        checkAtEnd(i);
    }

    @Test
    public void testReadWithNoOffset() throws IOException {
        // NOTE: this test is depending on SequenceInputStream's behavior to know
        //       how many times to call read() and how available() will reply.
        //       a change to SequenceInputStream's implementation could break
        //       this test.

        final byte[] contentsDest = new byte[CONTENTS.length()];
        final long contentsAvail = withSha1.available();
        final int numContentBytes = withSha1.read(contentsDest);
        assertEquals(contentsAvail, numContentBytes);

        final byte[] sha1Dest = new byte[SHA1.length()];
        final long sha1Avail = withSha1.available(); // available is zero here because the SequenceInputStream hasn't switched underlying streams yet. :(
        final int numSha1Bytes = withSha1.read(sha1Dest);  // note that this *doesn't* call read(byte[]) version. :(

        // copy the results from the two arrays into destination.
        int iDest = 0;
        for (int i=0; i < numContentBytes; i++) {
            dest[iDest] = contentsDest[i];
            iDest++;
        }
        for (int i=0; i < numSha1Bytes; i++) {
            dest[iDest] = sha1Dest[i];
            iDest++;
        }

        checkAtEnd(numContentBytes + numSha1Bytes);
    }

    @Test
    public void testSkip() throws IOException {
        final long numSkipped = withSha1.skip(dest.length);
        assertEquals(dest.length, numSkipped);
    }

    @Test
    public void test_forCoverage() throws IOException {
        while (withSha1.read() >= 0) {
            // not really checking the results since there's a time
            // in between the streams where the value is zero.
            //noinspection ResultOfMethodCallIgnored
            withSha1.available();
        }

        // grrr...i have to construct an instance too.  :(
        // i could make it an interface, but then the nested
        // class couldn't be private.  i suppose i could unnest it.
        new B2Sha1AppenderInputStream();
    }

    private void checkAtEnd(int numRead) throws IOException {
        // did we read as much as we expected?
        assertEquals(dest.length, numRead);

        // did we hit the end of the stream?
        assertEquals(-1, withSha1.read());

        // does the destination contain the expected contents?
        final String actual = new String(dest, B2StringUtil.UTF8);
        final String expected = CONTENTS + SHA1;
        assertEquals(expected, actual);
    }

    @After
    public void cleanup() throws IOException {
        B2IoUtils.closeQuietly(withSha1);

        original.close();
    }
}
