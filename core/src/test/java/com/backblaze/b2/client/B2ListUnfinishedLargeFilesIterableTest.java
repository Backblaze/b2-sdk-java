/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesRequest;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesResponse;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Iterator;
import java.util.List;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.makeVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class B2ListUnfinishedLargeFilesIterableTest extends B2BaseTest {
    private final String BUCKET_ID = bucketId(1);
    private final B2StorageClientImpl client = mock(B2StorageClientImpl.class);

    private final B2ListUnfinishedLargeFilesRequest TRIVIAL_REQUEST = B2ListUnfinishedLargeFilesRequest.builder(BUCKET_ID).build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testEmpty() throws B2Exception {
        final B2ListUnfinishedLargeFilesRequest request = TRIVIAL_REQUEST;

        // when asked, return an empty response, with no 'next's.
        final B2ListUnfinishedLargeFilesResponse emptyResponse = new B2ListUnfinishedLargeFilesResponse(B2Collections.listOf(), null);
        when(client.listUnfinishedLargeFiles(request)).thenReturn(emptyResponse);

        // the iterator should be empty.
        final Iterator<B2FileVersion> iter = new B2ListUnfinishedLargeFilesIterable(client, request).iterator();
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testOnePage() throws B2Exception {
        final B2ListUnfinishedLargeFilesRequest request = TRIVIAL_REQUEST;

        // when asked, return one answer with a few names and no 'next's.
        final List<B2FileVersion> names = B2Collections.listOf(makeVersion(1,1), makeVersion(2, 1));
        final B2ListUnfinishedLargeFilesResponse smallResponse = new B2ListUnfinishedLargeFilesResponse(names, null);
        when(client.listUnfinishedLargeFiles(request)).thenReturn(smallResponse);

        // iter should have two names.
        final Iterator<B2FileVersion> iter = new B2ListUnfinishedLargeFilesIterable(client, request).iterator();
        assertTrue(iter.hasNext());
        assertTrue(names.get(0) == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(names.get(1) == iter.next());
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testMultiplePages() throws B2Exception {
        // using some arguments here to make sure they're used in first request and that most are propagated.
        final B2ListUnfinishedLargeFilesRequest request = B2ListUnfinishedLargeFilesRequest
                .builder(BUCKET_ID)
                .setStartFileId("file/0000")
                .setMaxFileCount(106)
                .build();

        // when asked, return one answer with a few names and some nexts.
        final List<B2FileVersion> pageOneNames = B2Collections.listOf(makeVersion(1,1), makeVersion(2, 1));
        final B2ListUnfinishedLargeFilesResponse pageOneResponse = new B2ListUnfinishedLargeFilesResponse(pageOneNames, fileId(3));
        when(client.listUnfinishedLargeFiles(request)).thenReturn(pageOneResponse);

        final B2ListUnfinishedLargeFilesRequest pageTwoRequest = B2ListUnfinishedLargeFilesRequest
                .builder(BUCKET_ID)
                .setStartFileId(fileId(3))
                .setMaxFileCount(106)
                .build();
        final List<B2FileVersion> pageTwoNames = B2Collections.listOf(
                makeVersion(3,14),
                makeVersion(4, 15),
                makeVersion(5, 15));
        final B2ListUnfinishedLargeFilesResponse pageTwoResponse = new B2ListUnfinishedLargeFilesResponse(pageTwoNames, fileId(16));
        when(client.listUnfinishedLargeFiles(pageTwoRequest)).thenReturn(pageTwoResponse);

        // note that we expected to have more cuz pageTwoResponse had 'next's, but it turned out we didn't.
        final B2ListUnfinishedLargeFilesRequest pageThreeRequest = B2ListUnfinishedLargeFilesRequest
                .builder(BUCKET_ID)
                .setStartFileId(fileId(16))
                .setMaxFileCount(106)
                .build();
        final B2ListUnfinishedLargeFilesResponse pageThreeResponse = new B2ListUnfinishedLargeFilesResponse(B2Collections.listOf(), null);
        when(client.listUnfinishedLargeFiles(pageThreeRequest)).thenReturn(pageThreeResponse);

        // iter should have two pageOneNames.
        final Iterator<B2FileVersion> iter = new B2ListUnfinishedLargeFilesIterable(client, request).iterator();

        // first page.
        assertTrue(iter.hasNext());
        assertTrue(pageOneNames.get(0) == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(pageOneNames.get(1) == iter.next());
        assertTrue(iter.hasNext());

        // second page
        assertTrue(iter.hasNext());
        assertTrue(pageTwoNames.get(0) == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(pageTwoNames.get(1) == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(pageTwoNames.get(2) == iter.next());
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testBuilder() {
        B2ListUnfinishedLargeFilesRequest request3 = B2ListUnfinishedLargeFilesRequest
                .builder(BUCKET_ID)
                .setMaxFileCount(6)
                .build();
        assertEquals((Integer) 6, request3.getMaxFileCount());
    }

    @Test
    public void testForCoverage() {
        B2ListUnfinishedLargeFilesRequest request = B2ListUnfinishedLargeFilesRequest
                .builder(BUCKET_ID)
                .build();

        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
    }
}
