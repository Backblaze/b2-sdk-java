/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.exceptions.B2RuntimeException;
import com.backblaze.b2.client.structures.B2ListPartsRequest;
import com.backblaze.b2.client.structures.B2ListPartsResponse;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Iterator;
import java.util.List;

import static com.backblaze.b2.client.B2TestHelpers.SAMPLE_MD5;
import static com.backblaze.b2.client.B2TestHelpers.SAMPLE_SHA1;
import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class B2ListPartsIterableImplTest extends B2BaseTest {
    private final String LARGE_FILE_ID = fileId(1);
    private final B2StorageClientImpl client = mock(B2StorageClientImpl.class);

    private final B2ListPartsRequest TRIVIAL_REQUEST = B2ListPartsRequest.builder(LARGE_FILE_ID).build();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testEmpty() throws B2Exception {
        final B2ListPartsRequest request = TRIVIAL_REQUEST;

        // when asked, return an empty response, with no 'next's.
        final B2ListPartsResponse emptyResponse = new B2ListPartsResponse(B2Collections.listOf(), null);
        when(client.listParts(request)).thenReturn(emptyResponse);

        // the iterator should be empty.
        final Iterator<B2Part> iter = new B2ListPartsIterableImpl(client, request).iterator();
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testOnePage() throws B2Exception {
        final B2ListPartsRequest request = TRIVIAL_REQUEST;

        // when asked, return one answer with a few names and no 'next's.
        final List<B2Part> parts = B2Collections.listOf(part(1), part(2));
        final B2ListPartsResponse smallResponse = new B2ListPartsResponse(parts, null);
        when(client.listParts(request)).thenReturn(smallResponse);

        // iter should have two names.
        final Iterator<B2Part> iter = new B2ListPartsIterableImpl(client, request).iterator();
        assertTrue(iter.hasNext());
        assertTrue(parts.get(0) == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(parts.get(1) == iter.next());
        assertTrue(!iter.hasNext());
    }

    @Test
    public void testMultiplePages() throws B2Exception {
        // using some arguments here to make sure they're used in first request and that most are propagated.
        final B2ListPartsRequest request = B2ListPartsRequest
                .builder(LARGE_FILE_ID)
                .setStartPartNumber(0)
                .setMaxPartCount(106)
                .build();

        // when asked, return one answer with a few names and some nexts.
        final List<B2Part> pageOneParts = B2Collections.listOf(part(1), part(2));
        final B2ListPartsResponse pageOneResponse = new B2ListPartsResponse(pageOneParts, 3);
        when(client.listParts(request)).thenReturn(pageOneResponse);

        final B2ListPartsRequest pageTwoRequest = B2ListPartsRequest
                .builder(LARGE_FILE_ID)
                .setStartPartNumber(3)
                .setMaxPartCount(106)
                .build();
        final List<B2Part> pageTwoNames = B2Collections.listOf(
                part(3),
                part(4),
                part(5));
        final B2ListPartsResponse pageTwoResponse = new B2ListPartsResponse(pageTwoNames, 6);
        when(client.listParts(pageTwoRequest)).thenReturn(pageTwoResponse);

        // note that we expected to have more cuz pageTwoResponse had 'next's, but it turned out we didn't.
        final B2ListPartsRequest pageThreeRequest = B2ListPartsRequest
                .builder(LARGE_FILE_ID)
                .setStartPartNumber(6)
                .setMaxPartCount(106)
                .build();
        final B2ListPartsResponse pageThreeResponse = new B2ListPartsResponse(B2Collections.listOf(), null);
        when(client.listParts(pageThreeRequest)).thenReturn(pageThreeResponse);

        // iter should have two pageOneParts.
        final Iterator<B2Part> iter = new B2ListPartsIterableImpl(client, request).iterator();

        // first page.
        assertTrue(iter.hasNext());
        assertTrue(pageOneParts.get(0) == iter.next());
        assertTrue(iter.hasNext());
        assertTrue(pageOneParts.get(1) == iter.next());
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
    public void testForCoverage() {
        B2ListPartsRequest request = B2ListPartsRequest
                .builder(LARGE_FILE_ID)
                .build();

        //noinspection ResultOfMethodCallIgnored
        request.hashCode();
    }


    /////////////////////////////////////////////////////////
    // Let's test the exception cases in B2ListPartsIterableBase
    /////////////////////////////////////////////////////////

    @Test
    public void testCallingNextWhenHasNextIsFalse() throws B2Exception {
        // create an iterator and get to the where iter.hasNext() is false.

        // when asked, return an empty response, with no 'next's.
        final B2ListPartsResponse emptyResponse = new B2ListPartsResponse(B2Collections.listOf(), null);
        when(client.listParts(anyObject())).thenReturn(emptyResponse);

        // the iterator should be empty.
        final Iterator<B2Part> iter = new B2ListPartsIterableImpl(client, TRIVIAL_REQUEST).iterator();
        assertTrue(!iter.hasNext());

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("don't call when hasNext() returns false!");

        // call next despite iter saying there's no next!
        iter.next();
    }

    @Test
    public void testExceptionWhenConstructingIter() throws B2Exception {
        // when asked, throw.
        when(client.listParts(anyObject())).thenThrow(new B2InternalErrorException("test", "testing"));

        thrown.expect(B2RuntimeException.class);
        thrown.expectMessage("failed to create/advance iterator: testing");

        // this should throw.
        new B2ListPartsIterableImpl(client, TRIVIAL_REQUEST).iterator();
    }

    @Test
    public void testExceptionWhenAdvancingAfterConstructor() throws B2Exception {
        // return one page of answers, and say there's more to ask for.
        final B2ListPartsRequest firstRequest = TRIVIAL_REQUEST;
        final List<B2Part> firstPageNames = B2Collections.listOf(part(1), part(2));
        final B2ListPartsResponse firstPageResponse = new B2ListPartsResponse(firstPageNames, 3);
        when(client.listParts(firstRequest)).thenReturn(firstPageResponse);

        // throw when asked for the second page of answers.
        {
            final B2ListPartsRequest secondRequest = B2ListPartsRequest
                    .builder(LARGE_FILE_ID)
                    .setStartPartNumber(3)
                    .build();
            when(client.listParts(secondRequest)).thenThrow(new B2InternalErrorException("test", "testing"));
        }

        final Iterator<B2Part> iter = new B2ListPartsIterableImpl(client, firstRequest).iterator();

        assertTrue(iter.hasNext());
        assertTrue(firstPageNames.get(0) == iter.next());
        assertTrue(iter.hasNext());

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("failed to advance iterator: testing");

        assertTrue(firstPageNames.get(1) == iter.next());
    }

    private B2Part part(int i) {
        return new B2Part(
                LARGE_FILE_ID,
                i,
                i * 1000 * 1000,
                SAMPLE_SHA1,
                SAMPLE_MD5,
                i);
    }
}
