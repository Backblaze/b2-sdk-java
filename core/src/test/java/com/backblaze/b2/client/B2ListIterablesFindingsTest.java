/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.exceptions.B2RuntimeException;
import com.backblaze.b2.client.structures.B2ApplicationKey;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileNamesResponse;
import com.backblaze.b2.client.structures.B2ListKeysRequest;
import com.backblaze.b2.client.structures.B2ListKeysResponse;
import com.backblaze.b2.client.structures.B2ListPartsRequest;
import com.backblaze.b2.client.structures.B2ListPartsResponse;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import static com.backblaze.b2.client.B2TestHelpers.SAMPLE_MD5;
import static com.backblaze.b2.client.B2TestHelpers.SAMPLE_SHA1;
import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.client.B2TestHelpers.fileId;
import static com.backblaze.b2.client.B2TestHelpers.makeVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class B2ListIterablesFindingsTest extends B2BaseTest {
    private final B2StorageClientImpl client = mock(B2StorageClientImpl.class);

    private static B2ApplicationKey key(String id) {
        return new B2ApplicationKey("accountId", id, "name-" + id, new TreeSet<>(), "bucketId",
                "namePrefix", 12345678L, Collections.emptySet());
    }

    private static B2Part part(int i) {
        return new B2Part(fileId(1), i, i * 1000L, SAMPLE_SHA1, SAMPLE_MD5, i, null);
    }

    /** Finding C14 (keys): an empty page that is not the last must not end the listing. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC14_keysIteratorContinuesPastEmptyMiddlePage() throws B2Exception {
        final B2ListKeysRequest r1 = B2ListKeysRequest.builder().setMaxKeyCount(10).build();
        final B2ListKeysRequest r2 = B2ListKeysRequest.builder().setMaxKeyCount(10).setStartApplicationKeyId("id5").build();
        final B2ListKeysRequest r3 = B2ListKeysRequest.builder().setMaxKeyCount(10).setStartApplicationKeyId("id6").build();
        when(client.listKeys(eq(r1))).thenReturn(new B2ListKeysResponse(B2Collections.listOf(key("id1")), "id5"));
        when(client.listKeys(eq(r2))).thenReturn(new B2ListKeysResponse(new ArrayList<B2ApplicationKey>(), "id6"));
        when(client.listKeys(eq(r3))).thenReturn(new B2ListKeysResponse(B2Collections.listOf(key("id6")), null));

        final List<String> seen = new ArrayList<>();
        final Iterator<B2ApplicationKey> iter = new B2ListKeysIterator(client, r1);
        while (iter.hasNext()) {
            seen.add(iter.next().getApplicationKeyId());
        }
        assertEquals(B2Collections.listOf("id1", "id6"), seen);
    }

    /** Finding C14 (keys): next() on an exhausted iterator must throw NoSuchElementException per the Iterator contract. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC14_keysIteratorNextWhenExhaustedThrowsNoSuchElement() throws B2Exception {
        final B2ListKeysRequest r1 = B2ListKeysRequest.builder().build();
        when(client.listKeys(eq(r1))).thenReturn(new B2ListKeysResponse(new ArrayList<B2ApplicationKey>(), null));
        final Iterator<B2ApplicationKey> iter = new B2ListKeysIterator(client, r1);
        try {
            iter.next();
            fail("expected an exception");
        } catch (NoSuchElementException expected) {
            // expected
        } catch (RuntimeException other) {
            fail("wrong exception type: " + other);
        }
    }

    /** Finding C14 (parts): an empty page that is not the last must not end the listing. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC14_partsIteratorContinuesPastEmptyMiddlePage() throws B2Exception {
        final B2ListPartsRequest request = B2ListPartsRequest.builder(fileId(1)).build();
        final B2ListPartsRequest second = B2ListPartsRequest.builder(fileId(1)).setStartPartNumber(2).build();
        final B2ListPartsRequest third = B2ListPartsRequest.builder(fileId(1)).setStartPartNumber(3).build();
        // each page is only returned for the request that really asks for it, so a listing that does not
        // advance its continuation cursor cannot get the later pages.
        when(client.listParts(request)).thenReturn(new B2ListPartsResponse(B2Collections.listOf(part(1)), 2));
        when(client.listParts(second)).thenReturn(new B2ListPartsResponse(new ArrayList<B2Part>(), 3));
        when(client.listParts(third)).thenReturn(new B2ListPartsResponse(B2Collections.listOf(part(3)), null));

        final List<Integer> seen = new ArrayList<>();
        final Iterator<B2Part> iter = new B2ListPartsIterableImpl(client, request).iterator();
        while (iter.hasNext()) {
            seen.add(iter.next().getPartNumber());
        }
        assertEquals(B2Collections.listOf(1, 3), seen);
        // and the requests were exactly: first page, then start part 2, then start part 3
        verify(client).listParts(request);
        verify(client).listParts(second);
        verify(client).listParts(third);
        verifyNoMoreInteractions(client);
    }

    /**
     * Finding C13: when loading the next page fails, the item already in hand (the last one on the
     * current page) must still be returned; the failure should surface on the following hasNext().
     */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC13_lastItemOfPageIsNotLostWhenNextPageFails() throws B2Exception {
        final B2ListFileNamesRequest first = B2ListFileNamesRequest.builder(bucketId(1)).build();
        final List<B2FileVersion> page = B2Collections.listOf(makeVersion(1, 1), makeVersion(2, 1));
        when(client.listFileNames(first)).thenReturn(new B2ListFileNamesResponse(page, "file/next"));
        when(client.listFileNames(B2ListFileNamesRequest.builder(bucketId(1)).setStartFileName("file/next").build()))
                .thenThrow(new B2InternalErrorException("test", "testing"));

        final Iterator<B2FileVersion> iter = new B2ListFileNamesIterable(client, first).iterator();
        assertTrue(iter.hasNext());
        assertSame(page.get(0), iter.next());
        assertTrue(iter.hasNext());
        final B2FileVersion last;
        try {
            last = iter.next();
        } catch (B2RuntimeException e) {
            fail("the last item of the page was lost: next() threw " + e.getMessage());
            return;
        }
        assertSame(page.get(1), last);
        try {
            iter.hasNext();
            fail("the page-load failure should surface from hasNext()");
        } catch (B2RuntimeException expected) {
            assertTrue(expected.getMessage().contains("failed to advance iterator"));
        }
    }
}
