/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class B2FlushAndSyncFileOnCloseOutputStreamFindingsTest extends B2BaseTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** Finding C5: if flush fails during close(), the underlying handle must still be closed. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC5_failedFlushStillClosesHandle() throws Exception {
        final File f = folder.newFile("x");
        final boolean[] closed = {false};
        final FileOutputStream failingFlush = new FileOutputStream(f) {
            @Override
            public void flush() throws IOException {
                throw new IOException("simulated disk error");
            }

            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };
        final B2FlushAndSyncFileOnCloseOutputStream out = B2FlushAndSyncFileOnCloseOutputStream.create(failingFlush);
        try {
            out.close();
            fail("close should propagate the flush failure");
        } catch (IOException expected) {
            // expected
        }
        final boolean wasClosed = closed[0];
        failingFlush.close(); // test cleanup only, after the observation
        assertTrue("file handle was never closed after the flush failed", wasClosed);
    }
}
