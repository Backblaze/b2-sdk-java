/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.makeMd5;
import static com.backblaze.b2.client.B2TestHelpers.makeSha1;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class B2AlreadyStoredPartStorerTest extends B2BaseTest {

    private B2UploadListener uploadListener = mock(B2UploadListener.class);

    @Test
    public void testStorePart() {
        final B2Part part = new B2Part(fileId(1), 2, 10000000, makeSha1(1), makeMd5(2), 1111, null);
        final B2AlreadyStoredPartStorer partStorer = new B2AlreadyStoredPartStorer(part);
        final B2CancellationToken cancellationToken = new B2CancellationToken();
        assertEquals(part, partStorer.storePart(mock(B2LargeFileStorer.class), uploadListener, cancellationToken));
    }
}
