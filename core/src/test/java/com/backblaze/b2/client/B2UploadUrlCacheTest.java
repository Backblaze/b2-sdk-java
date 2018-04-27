/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.structures.B2UploadUrlResponse;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.client.B2TestHelpers.makeAuth;
import static com.backblaze.b2.client.B2TestHelpers.uploadUrlResponse;
import static com.backblaze.b2.client.B2UploadUrlCache.MAX_BUCKETS;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class B2UploadUrlCacheTest extends B2BaseTest {
    private final B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);
    private final B2AccountAuthorizationCache authCache = mock(B2AccountAuthorizationCache.class);

    private final B2UploadUrlCache uploadCache = new B2UploadUrlCache(webifier, authCache);

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testSimpleReuse() throws B2Exception {
        doReturn(makeAuth(1)).when(authCache).get();


        // nothing in the cache.  get the first answer from the webifier.
        final B2UploadUrlResponse response1_for_Bucket1 = uploadUrlResponse(bucketId(1), 1);
        doReturn(response1_for_Bucket1).when(webifier).getUploadUrl(anyObject(), anyObject());
        assertTrue(response1_for_Bucket1 == uploadCache.get(bucketId(1), false));

        // nothing in the cache, so it has to go to the webifier again.
        final B2UploadUrlResponse response2_for_Bucket1 = uploadUrlResponse(bucketId(1), 2);
        doReturn(response2_for_Bucket1).when(webifier).getUploadUrl(anyObject(), anyObject());
        assertTrue(response2_for_Bucket1 == uploadCache.get(bucketId(1), false));

        // return both.
        uploadCache.unget(response2_for_Bucket1);
        uploadCache.unget(response1_for_Bucket1);

        // ask again and get a cached answer.
        assertTrue(response2_for_Bucket1 == uploadCache.get(bucketId(1), false));

        // ask for another bucket and get a new answer from the webifier because it's for a different bucket.
        final B2UploadUrlResponse response1_for_Bucket2 = uploadUrlResponse(bucketId(2), 1);
        doReturn(response1_for_Bucket2).when(webifier).getUploadUrl(anyObject(), anyObject());
        assertTrue(response1_for_Bucket2 == uploadCache.get(bucketId(2), false));

        verify(webifier, times(3)).getUploadUrl(anyObject(), anyObject());
    }

    @Test
    public void testRetriesDontUseCachedAnswers() throws B2Exception {
        doReturn(makeAuth(1)).when(authCache).get();

        // add an answer to the cache that we would return for isRetry=false calls.
        final B2UploadUrlResponse response1 = uploadUrlResponse(bucketId(1), 1);
        uploadCache.unget(response1);

        // arrange for the webifier to return a different answer.
        final B2UploadUrlResponse response2 = uploadUrlResponse(bucketId(1), 3);
        doReturn(response2).when(webifier).getUploadUrl(anyObject(), anyObject());

        // isRetry = true, so don't return a cached answer.
        assertTrue(response2 == uploadCache.get(bucketId(1), true));

        // isRetry = false, so return a cached answer (just to prove one was there and we definitely skipped it for isRetry=true).
        assertTrue(response1 == uploadCache.get(bucketId(1), false));

        // we really only talked to the webifier once, right?
        verify(webifier, times(1)).getUploadUrl(anyObject(), anyObject());
    }

    @Test
    public void testMaxBuckets() throws B2Exception {
        // set up an answer from the webifier (even though we won't use it for a while)
        doReturn(uploadUrlResponse(bucketId(MAX_BUCKETS), 1))
                .when(webifier)
                .getUploadUrl(anyObject(), anyObject());

        // we don't want to hold buckets for an indefinite number of buckets,
        // so we discard the oldest ones if we exceed a maximum.  let's make
        // sure that we're doing that.

        // unget a response for every bucket up to the maximum number.
        for (int iBucket=0; iBucket < MAX_BUCKETS; iBucket++) {
            uploadCache.unget(uploadUrlResponse(bucketId(iBucket), 1));
        }

        // verify that we don't have to touch the network to get an entry for any of these buckets.
        // and put them back.
        for (int iBucket=0; iBucket < MAX_BUCKETS; iBucket++) {
            B2UploadUrlResponse response = uploadCache.get(bucketId(iBucket), false);
            uploadCache.unget(response);
        }
        verify(webifier, never()).getUploadUrl(anyObject(), anyObject());

        // unget one for one more bucket.
        // this should push the entry for one bucket out of the cache.
        uploadCache.unget(uploadUrlResponse(bucketId(MAX_BUCKETS + 1), 1));
        verify(webifier, never()).getUploadUrl(anyObject(), anyObject());


        // now we should have to hit the webifier for 1 of the buckets.
        for (int iBucket=0; iBucket < MAX_BUCKETS; iBucket++) {
            uploadCache.get(bucketId(iBucket), false);
        }
        verify(webifier, times(1)).getUploadUrl(anyObject(), anyObject());
    }

    @Test
    public void testExceptionFromAuthCache() throws B2Exception {
        final B2Exception e = new B2InternalErrorException("testing", "testing message");
        doThrow(e).when(authCache).get();

        thrown.expect(B2InternalErrorException.class);
        thrown.expectMessage("testing message");

        uploadCache.get(bucketId(1), false);
    }

    @Test
    public void testExceptionFromWebifier() throws B2Exception {
        final B2Exception e = new B2InternalErrorException("testing", "testing message");
        doThrow(e).when(webifier).getUploadUrl(anyObject(), anyObject());

        thrown.expect(B2InternalErrorException.class);
        thrown.expectMessage("testing message");

        uploadCache.get(bucketId(1), false);
    }
}
