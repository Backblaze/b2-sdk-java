/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2AccountAuthorization;

/**
 * The B2AccountAuthorizationCache holds the most recent account authorization
 * and can be cleared when it appears to have become invalid.
 * <p>
 * REQUIRES: the provided accountAuthorizer must be thread-safe.
 * <p>
 * THREAD-SAFETY: this class may be used from multiple threads safely.
 */
class B2AccountAuthorizationCache {
    private final B2StorageClientWebifier webifier;
    private final B2AccountAuthorizer accountAuthorizer;
    private volatile B2AccountAuthorization authorization;

    /**
     * The authorize() call from the authorizer should always
     * return an authorization for the same account. After
     * the first successful authorization, we hold on to the accountId
     * and use it to make sure we are always authenticating with the same account.
     */
    private volatile String accountId;

    B2AccountAuthorizationCache(B2StorageClientWebifier webifier,
                                B2AccountAuthorizer accountAuthorizer) {
        this.webifier = webifier;
        this.accountAuthorizer = accountAuthorizer;
    }


    /**
     * @return a B2AccountAuthorization.  it does *NOT* retry on its own.
     * <p>
     * THREADING: note that we intentionally block all other threads while
     *            waiting for an answer from the server.  if the first thread
     *            succeeds, they'll all benefit.  if one of them fails,
     *            the next one that asks will try again.  no need to ask
     *            multiple times in parallel.
     */
    B2AccountAuthorization get() throws B2Exception {
        // Store a local copy of authorization in case clear() is called concurrently after the null check
        B2AccountAuthorization localAuthorization = authorization;
        if (localAuthorization != null) {
            return localAuthorization;
        }

        synchronized (this) {
            localAuthorization = authorization;
            if (localAuthorization == null) {
                localAuthorization = accountAuthorizer.authorize(webifier);
                authorization = localAuthorization;

                final String accountIdFromAuthorization = localAuthorization.getAccountId();
                if (accountId == null) {
                    accountId = accountIdFromAuthorization;
                } else {
                    if (!accountId.equals(accountIdFromAuthorization)) {
                        throw new B2LocalException("unauthorized", "authorized as " + accountIdFromAuthorization +
                                "but previously authorized as accountId " + accountId);
                    }
                }
            }
            return localAuthorization;
        }
    }

    /**
     * Gets the stored accountId saved by authorizing.
     * If null, calls get() which does and authorization to get the accountId.
     * @return the accountId from a successful authorization
     * @throws B2Exception thrown from any B2Exception thrown during 'authorization' -> get()
     */
    String getAccountId() throws B2Exception {
        if (accountId != null) {
            return accountId;
        }

        synchronized (this) {
            if (accountId == null) {
                get();
            }
        }
        return accountId;
    }

     void clear() {
        authorization = null;
    }
}
