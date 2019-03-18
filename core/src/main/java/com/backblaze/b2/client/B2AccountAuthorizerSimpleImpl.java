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
    private final String applicationKeyId;
    private final String applicationKey;

    private B2AccountAuthorizerSimpleImpl(String applicationKeyId,
                                          String applicationKey) {
        B2Preconditions.checkArgument(applicationKeyId != null);
        B2Preconditions.checkArgument(applicationKey != null);
        this.applicationKeyId = applicationKeyId;
        this.applicationKey = applicationKey;
    }

    public static Builder builder(String applicationKeyId,
                                  String applicationKey) {
        return new Builder(applicationKeyId, applicationKey);
    }

    @Override
    public B2AccountAuthorization authorize(B2StorageClientWebifier webifier) throws B2Exception {
        final B2AuthorizeAccountRequest request = B2AuthorizeAccountRequest
                .builder(applicationKeyId, applicationKey)
                .build();
        return webifier.authorizeAccount(request);
    }


    public static class Builder {
        private final String applicationKeyId;
        private final String applicationKey;

        public Builder(String applicationKeyId,
                       String applicationKey) {
            this.applicationKeyId = applicationKeyId;
            this.applicationKey = applicationKey;
        }

        public B2AccountAuthorizerSimpleImpl build() {
            return new B2AccountAuthorizerSimpleImpl(applicationKeyId, applicationKey);
        }
    }
}
