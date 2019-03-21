/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.credentialsSources;

/**
 * Simple implementation of B2Credentials.
 */
public class B2CredentialsImpl implements B2Credentials {
    private final String applicationKeyId;
    private final String appKey;

    B2CredentialsImpl(String applicationKeyId,
                             String appKey) {
        this.applicationKeyId = applicationKeyId;
        this.appKey = appKey;
    }

    @Override
    public String getApplicationKeyId() {
        return applicationKeyId;
    }

    @Override
    public String getApplicationKey() {
        return appKey;
    }
}
