/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class B2UploadingPartStorerTest {

    @Test
    public void testStorePart() throws B2Exception {
        final B2ContentSource contentSource = mock(B2ContentSource.class);
        final B2UploadingPartStorer partStorer = new B2UploadingPartStorer(2, contentSource);
        final B2LargeFileStorer largeFileStorer = mock(B2LargeFileStorer.class);
        partStorer.storePart(largeFileStorer);
        verify(largeFileStorer).uploadPart(2, contentSource);
    }
}