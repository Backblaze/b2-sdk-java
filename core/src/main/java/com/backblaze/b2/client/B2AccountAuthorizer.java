/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2AccountAuthorization;

/**
 * B2AccountAuthorizer provides a mechanism to get an accountId and
 * a corresponding B2AccountAuthorization.  The accountId is expected
 * to never change.  The B2AccountAuthorization is required to make
 * a new authorization every time it is called.
 */
public interface B2AccountAuthorizer {
    /**
     * This will be called to get a new B2AccountAuthorization instance.
     *
     * @param webifier in case it's useful.  :)
     * @return an account authorization for the account this represents.
     * @throws B2Exception if it throws a B2UnauthorizedException, it must
     *                     ensure that the requestCategory is set to
     *                     ACCOUNT_AUTHORIZATION, so retries work properly.
     */
    B2AccountAuthorization authorize(B2StorageClientWebifier webifier) throws B2Exception;
}
