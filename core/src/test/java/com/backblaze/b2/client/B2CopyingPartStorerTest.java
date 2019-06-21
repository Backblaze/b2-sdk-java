/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.util.B2ByteRange;
import org.junit.Test;

import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.makeSha1;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class B2CopyingPartStorerTest {

    private static final String SOURCE_FILE_ID = fileId(1);
    private static final String DST_FILE_ID = fileId(2);
    private static final int PART_NUMBER = 2;
    private static final String SHA1 = makeSha1(2);

    private final B2Part part = new B2Part(DST_FILE_ID, PART_NUMBER, 5000000, SHA1, 2222);
    private final B2LargeFileStorer largeFileStorer = mock(B2LargeFileStorer.class);

    private final B2UploadListener uploadListener = mock(B2UploadListener.class);

    public B2CopyingPartStorerTest() throws B2Exception {
        when(largeFileStorer.copyPart(anyInt(), anyString(), anyObject(), anyObject())).thenReturn(part);
    }

    @Test
    public void testStorePart_noByteRange() throws B2Exception {
        final B2CopyingPartStorer partStorer = new B2CopyingPartStorer(PART_NUMBER, SOURCE_FILE_ID);

        assertEquals(part, partStorer.storePart(largeFileStorer, uploadListener));
        verify(largeFileStorer).copyPart(2, SOURCE_FILE_ID, null, uploadListener);
    }

    @Test
    public void testStorePart_byteRange() throws B2Exception {
        final B2ByteRange byteRange = B2ByteRange.between(1000000, 2000000);
        final B2CopyingPartStorer partStorer = new B2CopyingPartStorer(2, SOURCE_FILE_ID, byteRange);

        assertEquals(part, partStorer.storePart(largeFileStorer, uploadListener));
        verify(largeFileStorer).copyPart(2, SOURCE_FILE_ID, byteRange, uploadListener);
    }
}