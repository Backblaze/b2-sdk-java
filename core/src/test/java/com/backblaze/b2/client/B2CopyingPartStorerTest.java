/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.util.B2ByteRange;
import org.junit.Test;

import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class B2CopyingPartStorerTest {

    private static final String SOURCE_FILE_ID = fileId(1);

    private final B2LargeFileStorer largeFileStorer = mock(B2LargeFileStorer.class);

    @Test
    public void testStorePart_noByteRange() throws B2Exception {
        final B2CopyingPartStorer partStorer = new B2CopyingPartStorer(2, SOURCE_FILE_ID);

        partStorer.storePart(largeFileStorer);

        verify(largeFileStorer).copyPart(2, SOURCE_FILE_ID, null);
    }

    @Test
    public void testStorePart_byteRange() throws B2Exception {
        final B2ByteRange byteRange = B2ByteRange.between(1000000, 2000000);
        final B2CopyingPartStorer partStorer = new B2CopyingPartStorer(2, SOURCE_FILE_ID, byteRange);

        partStorer.storePart(largeFileStorer);

        verify(largeFileStorer).copyPart(2, SOURCE_FILE_ID, byteRange);
    }
}