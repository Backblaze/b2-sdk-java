/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentHandlers;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * This test *exercises* some of the code in B2ContentFileWriter.
 * The code in the base class is tested elsewhere.  Also, to avoid
 * actually writing to disk, it doesn't really write to disk.  Bummer, huh?
 * Hopefully there's more exercising of it elsewhere.
 */
public class B2ContentFileWriterTest extends B2BaseTest {
    private File FILE = new File("/tmp/outputFile");

    @Test
    public void testBuilder() {
        assertTrue(B2ContentFileWriter
                .builder(FILE)
                .build()
                .getVerifySha1ByRereadingFromDestination());
        assertTrue(!B2ContentFileWriter
                .builder(FILE)
                .setVerifySha1ByRereadingFromDestination(false)
                .build()
                .getVerifySha1ByRereadingFromDestination());
    }
}
