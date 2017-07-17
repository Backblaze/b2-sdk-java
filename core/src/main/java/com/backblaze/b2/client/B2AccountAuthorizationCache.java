/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2AccountAuthorization;

/**
 * The B2AccountAuthorizationCache holds the most recent account authorization
 * and can be cleared when it appears to have become invalid.
 *
 * REQUIRES: the provided accountAuthorizer must be thread-safe.
 *
 * THREAD-SAFETY: this class may be used from multiple threads safely.
 */
class B2AccountAuthorizationCache {
    private final B2StorageClientWebifier webifier;
    private final B2AccountAuthorizer accountAuthorizer;
    private B2AccountAuthorization authorization;

    B2AccountAuthorizationCache(B2StorageClientWebifier webifier,
                                B2AccountAuthorizer accountAuthorizer) {
        this.webifier = webifier;
        this.accountAuthorizer = accountAuthorizer;
    }


    /**
     * @return a B2AccountAuthorization.  it does *NOT* retry on its own.
     *
     * THREADING: note that we intentionally block all other threads while
     *            waiting for an answer from the server.  if the first thread
     *            succeeds, they'll all benefit.  if one of them fails,
     *            the next one that asks will try again.  no need to ask
     *            multiple times in parallel.
     */
    synchronized B2AccountAuthorization get() throws B2Exception {
        if (authorization == null) {
            authorization = accountAuthorizer.authorize(webifier);
        }
        return authorization;
    }

    synchronized void clear() {
        authorization = null;
    }
}
