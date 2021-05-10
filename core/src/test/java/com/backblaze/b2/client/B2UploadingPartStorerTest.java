/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.io.IOException;

import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.makeMd5;
import static com.backblaze.b2.client.B2TestHelpers.makeSha1;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class B2UploadingPartStorerTest extends B2BaseTest {

    private static final String FILE_ID = fileId(2);
    private static final int PART_NUMBER = 2;
    private static final String SHA1 = makeSha1(2);
    private static final String MD5 = makeMd5(2);

    private final B2Part part = new B2Part(FILE_ID, PART_NUMBER, 5000000, SHA1, MD5, 2222, null);

    private final B2UploadListener uploadListener = mock(B2UploadListener.class);

    @Test
    public void testStorePart() throws IOException, B2Exception {
        final B2ContentSource contentSource = mock(B2ContentSource.class);
        final B2UploadingPartStorer partStorer = new B2UploadingPartStorer(PART_NUMBER, contentSource);
        final B2LargeFileStorer largeFileStorer = mock(B2LargeFileStorer.class);

        when(largeFileStorer.uploadPart(anyInt(), anyObject(), anyObject())).thenReturn(part);

        assertEquals(part, partStorer.storePart(largeFileStorer, uploadListener));
        verify(largeFileStorer).uploadPart(2, contentSource, uploadListener);
    }
}
