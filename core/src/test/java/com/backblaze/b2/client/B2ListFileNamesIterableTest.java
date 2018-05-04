/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.exceptions.B2RuntimeException;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileNamesResponse;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Iterator;
import java.util.List;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.client.B2TestHelpers.fileName;
import static com.backblaze.b2.client.B2TestHelpers.makeVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class B2ListFileNamesIterableTest extends B2BaseTest {
    private final String BUCKET_ID = bucketId(1);
    private final B2StorageClientImpl client = mock(B2StorageClientImpl.class);

    private final B2ListFileNamesRequest TRIVIAL_REQUEST = B2ListFileNamesRequest.builder(BUCKET_ID).build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testEmpty() throws B2Exception {
        final B2ListFileNamesRequest request = TRIVIAL_REQUEST;

        // when asked, return an empty response, with no 'next's.
        final B2ListFileNamesResponse emptyResponse = new B2ListFileNamesResponse(B2Collections.listOf(), null);
        when(client.listFileNames(request)).thenReturn(emptyResponse);

        // the iterator should be empty.
        final Iterator<B2FileVersion> iter = new B2ListFileNamesIterable(client, request).iterator();
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testOnePage() throws B2Exception {
        final B2ListFileNamesRequest request = TRIVIAL_REQUEST;

        // when asked, return one answer with a few names and no 'next's.
        final List<B2FileVersion> names = B2Collections.listOf(makeVersion(1,1), makeVersion(2, 1));
        final B2ListFileNamesResponse smallResponse = new B2ListFileNamesResponse(names, null);
        when(client.listFileNames(request)).thenReturn(smallResponse);

        // iter should have two names.
        final Iterator<B2FileVersion> iter = new B2ListFileNamesIterable(client, request).iterator();
        assertTrue(iter.hasNext());
        assertTrue(names.get(0) == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(names.get(1) == iter.next());
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testMultiplePages() throws B2Exception {
        // using some arguments here to make sure they're used in first request and that most are propagated.
        final B2ListFileNamesRequest request = B2ListFileNamesRequest
                .builder(BUCKET_ID)
                .setStartFileName("file/0000")
                .setMaxFileCount(106)
                .setPrefix("file/")
                .setDelimiter("/")
                .build();

        // when asked, return one answer with a few names and some nexts.
        final List<B2FileVersion> pageOneNames = B2Collections.listOf(makeVersion(1,1), makeVersion(2, 1));
        final B2ListFileNamesResponse pageOneResponse = new B2ListFileNamesResponse(pageOneNames, fileName(3));
        when(client.listFileNames(request)).thenReturn(pageOneResponse);

        final B2ListFileNamesRequest pageTwoRequest = B2ListFileNamesRequest
                .builder(BUCKET_ID)
                .setStartFileName(fileName(3))
                .setMaxFileCount(106)
                .setPrefix("file/")
                .setDelimiter("/")
                .build();
        final List<B2FileVersion> pageTwoNames = B2Collections.listOf(
                makeVersion(3,14),
                makeVersion(4, 15),
                makeVersion(5, 15));
        final B2ListFileNamesResponse pageTwoResponse = new B2ListFileNamesResponse(pageTwoNames, fileName(16));
        when(client.listFileNames(pageTwoRequest)).thenReturn(pageTwoResponse);

        // note that we expected to have more cuz pageTwoResponse had 'next's, but it turned out we didn't.
        final B2ListFileNamesRequest pageThreeRequest = B2ListFileNamesRequest
                .builder(BUCKET_ID)
                .setStartFileName(fileName(16))
                .setMaxFileCount(106)
                .setPrefix("file/")
                .setDelimiter("/")
                .build();
        final B2ListFileNamesResponse pageThreeResponse = new B2ListFileNamesResponse(B2Collections.listOf(), null);
        when(client.listFileNames(pageThreeRequest)).thenReturn(pageThreeResponse);

        // iter should have two pageOneNames.
        final Iterator<B2FileVersion> iter = new B2ListFileNamesIterable(client, request).iterator();

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
        B2ListFileNamesRequest request = B2ListFileNamesRequest
                .builder(BUCKET_ID)
                .setWithinFolder("file/")
                .build();
        assertEquals("file/", request.getPrefix());
        assertEquals("/", request.getDelimiter());
        assertNull(request.getMaxFileCount());

        B2ListFileNamesRequest request2 = B2ListFileNamesRequest
                .builder(BUCKET_ID)
                .setStartFileName("file/0003")
                .build();
        assertEquals("file/0003", request2.getStartFileName());
        assertNull(request2.getDelimiter());

        B2ListFileNamesRequest request3 = B2ListFileNamesRequest
                .builder(BUCKET_ID)
                .setMaxFileCount(6)
                .build();
        assertEquals((Integer) 6, request3.getMaxFileCount());
    }

    @Test
    public void testForCoverage() {
        B2ListFileNamesRequest request = B2ListFileNamesRequest
                .builder(BUCKET_ID)
                .build();

        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
    }


    /////////////////////////////////////////////////////////
    // Let's test the exception cases in B2ListFilesIterableBase
    /////////////////////////////////////////////////////////

    @Test
    public void testCallingNextWhenHasNextIsFalse() throws B2Exception {
        // create an iterator and get to the where iter.hasNext() is false.

        // when asked, return an empty response, with no 'next's.
        final B2ListFileNamesResponse emptyResponse = new B2ListFileNamesResponse(B2Collections.listOf(), null);
        when(client.listFileNames(anyObject())).thenReturn(emptyResponse);

        // the iterator should be empty.
        final Iterator<B2FileVersion> iter = new B2ListFileNamesIterable(client, TRIVIAL_REQUEST).iterator();
        assertTrue(!iter.hasNext());

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("don't call when hasNext() returns false!");

        // call next despite iter saying there's no next!
        iter.next();
    }

    @Test
    public void testExceptionWhenConstructingIter() throws B2Exception {
        // when asked, throw.
        when(client.listFileNames(anyObject())).thenThrow(new B2InternalErrorException("test", "testing"));

        thrown.expect(B2RuntimeException.class);
        thrown.expectMessage("failed to create/advance iterator: testing");

        // this should throw.
        new B2ListFileNamesIterable(client, TRIVIAL_REQUEST).iterator();
    }

    @Test
    public void testExceptionWhenAdvancingAfterConstructor() throws B2Exception {
        // return one page of answers, and say there's more to ask for.
        final B2ListFileNamesRequest firstRequest = TRIVIAL_REQUEST;
        final List<B2FileVersion> firstPageNames = B2Collections.listOf(makeVersion(1, 1), makeVersion(2, 1));
        final B2ListFileNamesResponse firstPageResponse = new B2ListFileNamesResponse(firstPageNames, "file/throwWhenQueryWithThis");
        when(client.listFileNames(firstRequest)).thenReturn(firstPageResponse);

        // throw when asked for the second page of answers.
        {
            final B2ListFileNamesRequest secondRequest = B2ListFileNamesRequest
                    .builder(BUCKET_ID)
                    .setStartFileName("file/throwWhenQueryWithThis")
                    .build();
            when(client.listFileNames(secondRequest)).thenThrow(new B2InternalErrorException("test", "testing"));
        }

        final Iterator<B2FileVersion> iter = new B2ListFileNamesIterable(client, firstRequest).iterator();

        assertTrue(iter.hasNext());
        assertTrue(firstPageNames.get(0) == iter.next());
        assertTrue(iter.hasNext());

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("failed to advance iterator: testing");

        assertTrue(firstPageNames.get(1) == iter.next());
    }
}
