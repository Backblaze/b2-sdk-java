/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.backblaze.b2.client.B2TestHelpers.makeAuth;
import static com.backblaze.b2.client.B2TestHelpers.makeAuthWithAccountId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class B2AccountAuthorizationCacheTest extends B2BaseTest {
    private final B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);
    private final B2AccountAuthorizer authorizer = mock(B2AccountAuthorizer.class);

    private final String accountId = "accountId";

    private final B2AccountAuthorizationCache cache = new B2AccountAuthorizationCache(webifier, authorizer);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCache() throws B2Exception {
        // make the authorizer return auth1. and verify we get it.
        final B2AccountAuthorization auth1 = makeAuthWithAccountId(accountId, 1);
        doReturn(auth1).when(authorizer).authorize(webifier);
        assertTrue(cache.get() == auth1);

        // now make the authorizer return auth2 when it gets called.
        final B2AccountAuthorization auth2 = makeAuthWithAccountId(accountId, 2);
        doReturn(auth2).when(authorizer).authorize(webifier);

        // we should keep getting auth1...because we're not calling the authorizer again.
        {
            assertTrue(cache.get() == auth1);
            assertEquals(auth1.getAccountId(), cache.getAccountId());
            assertTrue(cache.get() == auth1);
            assertEquals(auth1.getAccountId(), cache.getAccountId());
            assertTrue(cache.get() == auth1);
            assertEquals(auth1.getAccountId(), cache.getAccountId());

            // ...we have still only called the authorizer once!
            verify(authorizer, times(1)).authorize(webifier);
        }

        // clearing the cache should cause us to fetch again on the next get().
        // note: clearing the cache does NOT change or reset the accountID
        cache.clear();

        //cache accountId survives clearing
        assertTrue(cache.getAccountId().equals(accountId));
        verify(authorizer, times(1)).authorize(webifier);

        // now we should keep getting auth2...
        {
            assertTrue(cache.get() == auth2);
            assertTrue(cache.get() == auth2);
            assertTrue(cache.get() == auth2);
            assertTrue(cache.get() == auth2);

            // accountId from cache matches both authorization calls
            assertTrue(cache.getAccountId().equals(auth1.getAccountId()));
            assertTrue(cache.getAccountId().equals(auth2.getAccountId()));

            // but the actual auth should not be equal, since we made a new call
            assertFalse(cache.get() == auth1);

            // ...and only have called the authorizer one more time!
            verify(authorizer, times(2)).authorize(webifier);
        }
    }

    @Test
    public void testMismatchAccountId() throws B2Exception {
        final B2AccountAuthorization auth1 = makeAuthWithAccountId(accountId, 1);
        doReturn(auth1).when(authorizer).authorize(webifier);
        assertTrue(cache.get() == auth1);

        //clear to force another authorize()
        cache.clear();

        thrown.expect(B2LocalException .class);
        final B2AccountAuthorization auth2 = makeAuthWithAccountId("anotherAccountId", 2);
        doReturn(auth2).when(authorizer).authorize(webifier);

        //throws because accountId's mismatch
        cache.get();

    }

    @Test
    public void testGetAccountIdCallsGet() throws B2Exception {
        final B2AccountAuthorization auth1 = makeAuthWithAccountId(accountId, 1);
        doReturn(auth1).when(authorizer).authorize(webifier);
        assertTrue(cache.getAccountId().equals(accountId));
        verify(authorizer, times(1)).authorize(webifier);

        //verify we only called authorize once when getting the accountId
        assertTrue(cache.getAccountId().equals(accountId));
        verify(authorizer, times(1)).authorize(webifier);
    }

    @Test
    public void testException() throws B2Exception {
        final B2Exception e = new B2InternalErrorException("testing", "testing message");
        doThrow(e).when(authorizer).authorize(anyObject());

        thrown.expect(B2InternalErrorException.class);
        thrown.expectMessage("testing message");

        cache.get();
    }

    @Test
    public void test_forCoverage() {
        B2AccountAuthorization a = makeAuth(1);
        B2AccountAuthorization b = makeAuth(1);
        assertEquals(a, b);
        //noinspection ResultOfMethodCallIgnored
        a.hashCode();

        assertEquals("B2AccountAuthorization{accountId='1', authorizationToken='accountToken1', apiUrl='apiUrl1', downloadUrl='downloadUrl1', recommendedPartSize=1000, absoluteMinimumPartSize=100}", a.toString());
    }

}
