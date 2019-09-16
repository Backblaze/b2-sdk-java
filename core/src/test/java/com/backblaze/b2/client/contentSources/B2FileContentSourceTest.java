/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentSources;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.backblaze.b2.client.B2TestHelpers.SAMPLE_SHA1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class B2FileContentSourceTest extends B2BaseTest {
    // rather than make a file on disk (and making the unit test depend on the filesystem)
    // i'm just going to try to use a non-existent file and check that i get reasonable
    // exceptions.
    private static final File file = new File("/this/file/doesnt/exist.txt");
    private static final B2ContentSource contentSource = B2FileContentSource.build(file);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetSha1OrNull() throws IOException {
        // for a contentSource that doesn't specify the sha1
        assertNull(contentSource.getSha1OrNull());

        // for a contentSource that *does* specify the sha1
        final B2ContentSource withSha1 = B2FileContentSource
                .builder(file)
                .setSha1(SAMPLE_SHA1)
                .build();
        assertEquals(SAMPLE_SHA1, withSha1.getSha1OrNull());
    }

    @Test
    public void testGetSrcLastModifiedReturnsZeroCuzFileDoesntExist() throws IOException {
        assertEquals((Long) 0L, contentSource.getSrcLastModifiedMillisOrNull());
    }

    @Test
    public void testGetContentLengthReturnsZeroCuzFileDoesntExist() throws IOException {
        assertEquals(0, contentSource.getContentLength());
    }

    @Test
    public void testCreateInputStreamTriesToOpenTheFile() throws IOException, B2Exception {
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage(file + " (No such file or directory)");
        contentSource.createInputStream();
    }
}
