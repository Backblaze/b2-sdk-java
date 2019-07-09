/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2AccountAuthorization;
import com.backblaze.b2.client.structures.B2Allowed;
import com.backblaze.b2.client.structures.B2AuthorizeAccountRequest;
import com.backblaze.b2.util.B2Preconditions;

/**
 * B2AccountAuthorizerV5AuthImpl is used when the user has authenticated
 * with B1.
 * public B2AccountAuthorization(String accountId,
 String authorizationToken,
 String apiUrl,
 String downloadUrl,
 long recommendedPartSize,
 long absoluteMinimumPartSize,
 B2Allowed allowed) {
 */
public class B2AccountAuthorizerV5AuthImpl implements B2AccountAuthorizer {
    private final String accountID;
    private final String authorizationToken;
    private final String apiUrl;
    private final String downloadUrl;
    private final long recommendedPartSize;
    private final long absoluteMinimumPartSize;
    private final B2Allowed b2Allowed;

    @Override
    public B2AccountAuthorization authorize(B2StorageClientWebifier webifier) throws B2Exception {
        B2AccountAuthorization b2AccountAuthorization = new B2AccountAuthorization(
                accountID,
                authorizationToken,
                apiUrl,
                downloadUrl,
                recommendedPartSize,
                absoluteMinimumPartSize,
                b2Allowed );
        return b2AccountAuthorization;
    }
    public String getAccountID() {
        return accountID;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public long getRecommendedPartSize() {
        return recommendedPartSize;
    }

    public long getAbsoluteMinimumPartSize() {
        return absoluteMinimumPartSize;
    }

    public B2Allowed getB2Allowed() {
        return b2Allowed;
    }

    private B2AccountAuthorizerV5AuthImpl(Builder builder) {
        accountID = builder.accountID;
        authorizationToken = builder.authorizationToken;
        apiUrl = builder.apiUrl;
        downloadUrl = builder.downloadUrl;
        recommendedPartSize = builder.recommendedPartSize;
        absoluteMinimumPartSize = builder.absoluteMinimumPartSize;
        b2Allowed = builder.b2Allowed;
    }

    public static final class Builder {
        private String accountID;
        private String authorizationToken;
        private String apiUrl;
        private String downloadUrl;
        private long recommendedPartSize;
        private long absoluteMinimumPartSize;
        private B2Allowed b2Allowed;

        public Builder() {
        }

        public Builder(B2AccountAuthorizerV5AuthImpl copy) {
            this.accountID = copy.getAccountID();
            this.authorizationToken = copy.getAuthorizationToken();
            this.apiUrl = copy.getApiUrl();
            this.downloadUrl = copy.getDownloadUrl();
            this.recommendedPartSize = copy.getRecommendedPartSize();
            this.absoluteMinimumPartSize = copy.getAbsoluteMinimumPartSize();
            this.b2Allowed = copy.getB2Allowed();
        }

        public Builder setAccountID(String accountID) {
            this.accountID = accountID;
            return this;
        }

        public Builder setAuthorizationToken(String authorizationToken) {
            this.authorizationToken = authorizationToken;
            return this;
        }

        public Builder setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }

        public Builder setRecommendedPartSize(long recommendedPartSize) {
            this.recommendedPartSize = recommendedPartSize;
            return this;
        }

        public Builder setAbsoluteMinimumPartSize(long absoluteMinimumPartSize) {
            this.absoluteMinimumPartSize = absoluteMinimumPartSize;
            return this;
        }

        public Builder setB2Allowed(B2Allowed b2Allowed) {
            this.b2Allowed = b2Allowed;
            return this;
        }

        public B2AccountAuthorizerV5AuthImpl build() {
            return new B2AccountAuthorizerV5AuthImpl(this);
        }
    }
}
