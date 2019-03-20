/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.credentialsSources;

/**
 * Credentials for accessing B2's APIs.
 */
public interface B2Credentials {
    /**
     * @return the accountId to use for b2_authorize_account.
     */
    @Deprecated
    String getAccountId();

    /**
     * @return the applicationKeyId to use for b2_authorize_account.
     */
    String getApplicationKeyId();

    /**
     * @return the applicationKey to use for b2_authorize_account.
     */
    String getApplicationKey();
}
