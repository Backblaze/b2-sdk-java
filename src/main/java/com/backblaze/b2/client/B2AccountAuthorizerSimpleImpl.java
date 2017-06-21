/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2AuthorizeAccountRequest;
import com.backblaze.b2.util.B2Preconditions;

/**
 * B2AccountAuthorizerSimpleImpl is the usual implementation of authorizing.
 * Almost everyone is expected to use this right now.
 */
public class B2AccountAuthorizerSimpleImpl implements B2AccountAuthorizer {
    private final String accountId;
    private final String applicationKey;

    private B2AccountAuthorizerSimpleImpl(String accountId,
                                          String applicationKey) {
        B2Preconditions.checkArgument(accountId != null);
        B2Preconditions.checkArgument(applicationKey != null);
        this.accountId = accountId;
        this.applicationKey = applicationKey;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public static Builder builder(String accountId,
                                  String applicationKey) {
        return new Builder(accountId, applicationKey);
    }

    @Override
    public B2AccountAuthorization authorize(B2StorageClientWebifier webifier) throws B2Exception {
        final B2AuthorizeAccountRequest request = B2AuthorizeAccountRequest
                .builder(accountId, applicationKey)
                .build();
        return webifier.authorizeAccount(request);
    }


    public static class Builder {
        private final String accountId;
        private final String applicationKey;

        public Builder(String accountId,
                       String applicationKey) {
            this.accountId = accountId;
            this.applicationKey = applicationKey;
        }

        public B2AccountAuthorizerSimpleImpl build() {
            return new B2AccountAuthorizerSimpleImpl(accountId, applicationKey);
        }
    }
}
