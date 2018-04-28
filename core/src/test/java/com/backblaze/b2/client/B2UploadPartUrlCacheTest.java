/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.structures.B2UploadPartUrlResponse;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.client.B2TestHelpers.makeAuth;
import static com.backblaze.b2.client.B2TestHelpers.uploadPartUrlResponse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class B2UploadPartUrlCacheTest extends B2BaseTest {
    private static final String LARGE_FILE_ID_1 = "4_zb330e285948b7a6d4b1b0712_f2000000000000001_d20150314_m111111_c001_v1234567_t6789";
    //private static final String LARGE_FILE_ID_2 = "4_zb330e285948b7a6d4b1b0712_f2000000000000002_d20150314_m222222_c001_v1234567_t6789";

    private final B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);
    private final B2AccountAuthorizationCache authCache = mock(B2AccountAuthorizationCache.class);

    private final B2UploadPartUrlCache uploadPartCache = new B2UploadPartUrlCache(webifier, authCache, LARGE_FILE_ID_1);

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testSimpleReuse() throws B2Exception {
        doReturn(makeAuth(1)).when(authCache).get();


        // nothing in the cache.  get the first answer from the webifier.
        final B2UploadPartUrlResponse response1 = uploadPartUrlResponse(bucketId(1), 1);
        doReturn(response1).when(webifier).getUploadPartUrl(anyObject(), anyObject());
        assertTrue(response1 == uploadPartCache.get(false));

        // nothing in the cache, so it has to go to the webifier again.
        final B2UploadPartUrlResponse response2 = uploadPartUrlResponse(bucketId(1), 2);
        doReturn(response2).when(webifier).getUploadPartUrl(anyObject(), anyObject());
        assertTrue(response2 == uploadPartCache.get(false));

        // return both.
        uploadPartCache.unget(response2);
        uploadPartCache.unget(response1);

        // ask again and get a cached answer.
        assertTrue(response2 == uploadPartCache.get(false));

        verify(webifier, times(2)).getUploadPartUrl(anyObject(), anyObject());
    }

    @Test
    public void testRetriesDontUseCachedAnswers() throws B2Exception {
        doReturn(makeAuth(1)).when(authCache).get();

        // add an answer to the cache that we would return for isRetry=false calls.
        final B2UploadPartUrlResponse response1 = uploadPartUrlResponse(bucketId(1), 1);
        uploadPartCache.unget(response1);

        // arrange for the webifier to return a different answer.
        final B2UploadPartUrlResponse response2 = uploadPartUrlResponse(bucketId(1), 3);
        doReturn(response2).when(webifier).getUploadPartUrl(anyObject(), anyObject());

        // isRetry = true, so don't return a cached answer.
        assertTrue(response2 == uploadPartCache.get(true));

        // isRetry = false, so return a cached answer (just to prove one was there and we definitely skipped it for isRetry=true).
        assertTrue(response1 == uploadPartCache.get(false));

        // we really only talked to the webifier once, right?
        verify(webifier, times(1)).getUploadPartUrl(anyObject(), anyObject());
    }

    @Test
    public void testExceptionFromAuthCache() throws B2Exception {
        final B2Exception e = new B2InternalErrorException("testing", "testing message");
        doThrow(e).when(authCache).get();

        thrown.expect(B2InternalErrorException.class);
        thrown.expectMessage("testing message");

        uploadPartCache.get(false);
    }

    @Test
    public void testExceptionFromWebifier() throws B2Exception {
        final B2Exception e = new B2InternalErrorException("testing", "testing message");
        doThrow(e).when(webifier).getUploadPartUrl(anyObject(), anyObject());

        thrown.expect(B2InternalErrorException.class);
        thrown.expectMessage("testing message");

        uploadPartCache.get(false);
    }
}
