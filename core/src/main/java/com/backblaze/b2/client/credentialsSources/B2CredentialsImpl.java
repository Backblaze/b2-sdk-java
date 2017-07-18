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
    private final String appKey;

    B2CredentialsImpl(String accountId,
                             String appKey) {
        this.accountId = accountId;
        this.appKey = appKey;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public String getApplicationKey() {
        return appKey;
    }
}
