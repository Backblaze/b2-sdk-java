/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.credentialsSources;

/**
 * Simple implementation of B2Credentials.
 */
public class B2CredentialsImpl implements B2Credentials {
    private final String accountId;
    private final String appKeyId;
    private final String appKey;

    B2CredentialsImpl(String accountId,
                             String appKey) {

        // for now consider any accountId an appKeyId
        this.appKeyId = accountId;

        this.accountId = accountId;
        this.appKey = appKey;
    }

    @Override
    @Deprecated
    public String getAccountId() {
        return accountId;
    }

    @Override
    public String getApplicationKeyId() {
        return appKeyId;
    }

    @Override
    public String getApplicationKey() {
        return appKey;
    }
}
