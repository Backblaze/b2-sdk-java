/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsResponse;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.fileName;
import static com.backblaze.b2.client.B2TestHelpers.makeVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class B2ListFileVersionsIterableTest extends B2BaseTest {
    private final String BUCKET_ID = bucketId(1);
    private final B2StorageClientImpl client = mock(B2StorageClientImpl.class);

    @Test
    public void testEmpty() throws B2Exception {
        final B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .build();

        // when asked, return an empty response, with no 'next's.
        final B2ListFileVersionsResponse emptyResponse = new B2ListFileVersionsResponse(B2Collections.listOf(), null, null);
        when(client.listFileVersions(request)).thenReturn(emptyResponse);

        // the iterator should be empty.
        final Iterator<B2FileVersion> iter = new B2ListFileVersionsIterable(client, request).iterator();
        //noinspection SimplifiableJUnitAssertion
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testOnePage() throws B2Exception {
        final B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .build();

        // when asked, return one answer with a few versions and no 'next's.
        final List<B2FileVersion> versions = B2Collections.listOf(makeVersion(1,1), makeVersion(2, 1));
        final B2ListFileVersionsResponse smallResponse = new B2ListFileVersionsResponse(versions, null, null);
        when(client.listFileVersions(request)).thenReturn(smallResponse);

        // iter should have two versions.
        final Iterator<B2FileVersion> iter = new B2ListFileVersionsIterable(client, request).iterator();
        assertTrue(iter.hasNext());
        assertSame(versions.get(0), iter.next());
        assertTrue(iter.hasNext());
        assertSame(versions.get(1), iter.next());
        //noinspection SimplifiableJUnitAssertion
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testEmptyPageWithNextFileNameAndVersion() throws B2Exception {
        final B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .setStart("file/0000", fileId(0))
                .setMaxFileCount(106)
                .setPrefix("file/")
                .setDelimiter("/")
                .build();

        // First page is empty, but has a nextFileName
        final B2ListFileVersionsResponse pageOneResponse = new B2ListFileVersionsResponse(B2Collections.listOf(), "file/0000", fileId(3000013));
        when(client.listFileVersions(request)).thenReturn(pageOneResponse);

        // Second page has actual results
        final B2ListFileVersionsRequest pageTwoRequest = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .setStart("file/0000", fileId(3000013))
                .setMaxFileCount(106)
                .setPrefix("file/")
                .setDelimiter("/")
                .build();
        final List<B2FileVersion> pageTwoVersions = B2Collections.listOf(
                makeVersion(3000000,3000014),
                makeVersion(4000000, 3000015),
                makeVersion(5000000, 3000015));
        final B2ListFileVersionsResponse pageTwoResponse = new B2ListFileVersionsResponse(pageTwoVersions, null, null);
        when(client.listFileVersions(pageTwoRequest)).thenReturn(pageTwoResponse);

        final Iterator<B2FileVersion> iter = new B2ListFileVersionsIterable(client, request).iterator();

        // first page.
        assertTrue(iter.hasNext());

        // second page
        assertSame(pageTwoVersions.get(0), iter.next());
        assertTrue(iter.hasNext());
        assertSame(pageTwoVersions.get(1), iter.next());
        assertTrue(iter.hasNext());
        assertSame(pageTwoVersions.get(2), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMultiplePages() throws B2Exception {
        // using some arguments here to make sure they're used in first request and that most are propagated.
        final B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .setStart("file/0000", fileId(0))
                .setMaxFileCount(106)
                .setPrefix("file/")
                .setDelimiter("/")
                .build();

        // when asked, return one answer with a few versions and some nexts.
        final List<B2FileVersion> pageOneVersions = B2Collections.listOf(makeVersion(1, 1), makeVersion(2, 1));
        final B2ListFileVersionsResponse pageOneResponse = new B2ListFileVersionsResponse(pageOneVersions, fileName(3), fileId(13));
        when(client.listFileVersions(request)).thenReturn(pageOneResponse);

        final B2ListFileVersionsRequest pageTwoRequest = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .setStart(fileName(3), fileId(13))
                .setMaxFileCount(106)
                .setPrefix("file/")
                .setDelimiter("/")
                .build();
        final List<B2FileVersion> pageTwoVersions = B2Collections.listOf(
                makeVersion(3,14),
                makeVersion(4, 15),
                makeVersion(5, 15));
        final B2ListFileVersionsResponse pageTwoResponse = new B2ListFileVersionsResponse(pageTwoVersions, fileName(16), fileId(5));
        when(client.listFileVersions(pageTwoRequest)).thenReturn(pageTwoResponse);

        // note that we expected to have more cuz pageTwoResponse had 'next's, but it turned out we didn't.
        final B2ListFileVersionsRequest pageThreeRequest = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .setStart(fileName(16), fileId(5))
                .setMaxFileCount(106)
                .setPrefix("file/")
                .setDelimiter("/")
                .build();
        final B2ListFileVersionsResponse pageThreeResponse = new B2ListFileVersionsResponse(B2Collections.listOf(), null, null);
        when(client.listFileVersions(pageThreeRequest)).thenReturn(pageThreeResponse);

        // iter should have two pageOneVersions.
        final Iterator<B2FileVersion> iter = new B2ListFileVersionsIterable(client, request).iterator();

        // first page.
        assertTrue(iter.hasNext());
        assertSame(pageOneVersions.get(0), iter.next());
        assertTrue(iter.hasNext());
        assertSame(pageOneVersions.get(1), iter.next());
        assertTrue(iter.hasNext());

        // second page
        //noinspection ConstantConditions
        assertTrue(iter.hasNext());
        assertSame(pageTwoVersions.get(0), iter.next());
        assertTrue(iter.hasNext());
        assertSame(pageTwoVersions.get(1), iter.next());
        assertTrue(iter.hasNext());
        assertSame(pageTwoVersions.get(2), iter.next());
        //noinspection SimplifiableJUnitAssertion
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testOkForNextFileIdToBeNull() throws B2Exception {
        final B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .build();

        // when asked, return one answer with a few versions and no 'next's.
        final List<B2FileVersion> versions = B2Collections.listOf(makeVersion(1,1), makeVersion(2, 1));
        final B2ListFileVersionsResponse smallResponse = new B2ListFileVersionsResponse(versions, fileName(2), null);
        when(client.listFileVersions(request)).thenReturn(smallResponse);

        // note that we expected to have more cuz pageTwoResponse had nextFileName, but it turned out we didn't.
        final B2ListFileVersionsRequest pageTwoRequest = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .setStart(fileName(2), null)
                .build();
        final B2ListFileVersionsResponse pageTwoResponse = new B2ListFileVersionsResponse(B2Collections.listOf(), null, null);
        when(client.listFileVersions(pageTwoRequest)).thenReturn(pageTwoResponse);


        // iter should have two versions.
        final Iterator<B2FileVersion> iter = new B2ListFileVersionsIterable(client, request).iterator();
        assertTrue(iter.hasNext());
        assertSame(versions.get(0), iter.next());
        assertTrue(iter.hasNext());
        assertSame(versions.get(1), iter.next());
        //noinspection SimplifiableJUnitAssertion
        assertTrue(!iter.hasNext());
    }


    @Test
    public void testBuilder() {
        B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .setWithinFolder("file/")
                .build();
        assertEquals("file/", request.getPrefix());
        assertEquals("/", request.getDelimiter());
        assertNull(request.getMaxFileCount());

        B2ListFileVersionsRequest request2 = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .setStartFileName("file/0003")
                .build();
        assertEquals("file/0003", request2.getStartFileName());
        assertNull(request2.getDelimiter());
        assertNull(request2.getStartFileId());

        B2ListFileVersionsRequest request3 = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .setMaxFileCount(6)
                .build();
        assertEquals((Integer) 6, request3.getMaxFileCount());
    }

    @Test
    public void testForCoverage() {
        B2ListFileVersionsRequest request = B2ListFileVersionsRequest
                .builder(BUCKET_ID)
                .build();

        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
    }
}
