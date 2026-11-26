/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static com.backblaze.b2.client.B2TestHelpers.makeAuthWithAccountId;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class B2AccountAuthorizationCacheFindingsTest extends B2BaseTest {
    private final B2StorageClientWebifier webifier = mock(B2StorageClientWebifier.class);
    private final B2AccountAuthorizer authorizer = mock(B2AccountAuthorizer.class);
    private final B2AccountAuthorizationCache cache = new B2AccountAuthorizationCache(webifier, authorizer);

    private void authorizeThenSwitchToOtherAccount() throws B2Exception {
        doReturn(makeAuthWithAccountId("accountId", 1)).when(authorizer).authorize(webifier);
        cache.get();
        cache.clear();
        doReturn(makeAuthWithAccountId("anotherAccountId", 2)).when(authorizer).authorize(webifier);
    }

    /** Finding C1 (and C2: the existing test never made a second get() call). */
    @Test
    public void testFindingC1_C2_mismatchedAccountAuthIsNotStoredAndServedOnNextGet() throws B2Exception {
        authorizeThenSwitchToOtherAccount();
        try {
            cache.get();
            fail("first get() should throw because the account id changed");
        } catch (B2LocalException expected) {
            // expected
        }

        // the second get() must not hand out the login of the other account.
        try {
            final B2AccountAuthorization second = cache.get();
            fail("second get() returned the authorization of a different account: " + second.getAccountId());
        } catch (B2LocalException expected) {
            // expected
        }
    }

    /** Finding C1: the error text runs two words together ("...Idbut previously..."). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC1_mismatchMessageHasSpaceBeforeBut() throws B2Exception {
        authorizeThenSwitchToOtherAccount();
        try {
            cache.get();
            fail("should have thrown");
        } catch (B2LocalException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("anotherAccountId but previously"));
        }
    }
}
