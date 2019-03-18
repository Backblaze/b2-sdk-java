/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.structures.B2TestMode;
import com.backblaze.b2.util.B2Preconditions;

import java.io.Closeable;
import java.util.Objects;

/**
 * B2ClientConfig describes the various settings the B2 clients need.
 *      * @param executor the executor to use for doing things in parallel (such as uploading large file parts)

 */
public class B2ClientConfig {
    private final B2AccountAuthorizer accountAuthorizer;
    private final String userAgent;
    private final String masterUrl;
    private final B2TestMode testModeOrNull;

    private B2ClientConfig(B2AccountAuthorizer accountAuthorizer,
                           String userAgent,
                           String masterUrl,
                           B2TestMode testModeOrNull) {
        B2Preconditions.checkArgument(userAgent != null && userAgent.length() > 0);
        this.accountAuthorizer = accountAuthorizer;
        this.userAgent = userAgent;
        this.masterUrl = masterUrl;
        this.testModeOrNull = testModeOrNull;
    }

    public B2AccountAuthorizer getAccountAuthorizer() {
        return accountAuthorizer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getMasterUrl() {
        return masterUrl;
    }

    public B2TestMode getTestModeOrNull() {
        return testModeOrNull;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2ClientConfig that = (B2ClientConfig) o;
        return Objects.equals(getAccountAuthorizer(), that.getAccountAuthorizer()) &&
                Objects.equals(getUserAgent(), that.getUserAgent()) &&
                Objects.equals(getMasterUrl(), that.getMasterUrl()) &&
                getTestModeOrNull() == that.getTestModeOrNull();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAccountAuthorizer(), getUserAgent(), getMasterUrl(), getTestModeOrNull());
    }

    public static Builder builder(B2AccountAuthorizer accountAuthorizer, String userAgent) {
        return new Builder(accountAuthorizer, userAgent);
    }

    public static Builder builder(String applicationKeyId, String applicationKey, String userAgent) {
        return new Builder(B2AccountAuthorizerSimpleImpl
                .builder(applicationKeyId, applicationKey)
                .build(),
                userAgent);
    }

    /**
     * The Builder for building a B2ClientConfig.
     */
    public static class Builder {
        private final B2AccountAuthorizer accountAuthorizer;
        private final String userAgent;
        private String masterUrl;
        private B2TestMode testModeOrNull;

        public Builder(B2AccountAuthorizer accountAuthorizer,
                       String userAgent) {
            this.accountAuthorizer = accountAuthorizer;
            this.userAgent = userAgent;
        }

        public Builder setMasterUrl(String masterUrl) {
            this.masterUrl = masterUrl;
            return this;
        }

        public Builder setTestModeOrNull(B2TestMode testModeOrNull) {
            this.testModeOrNull = testModeOrNull;
            return this;
        }

        public B2ClientConfig build() {
            return new B2ClientConfig(
                    accountAuthorizer,
                    userAgent,
                    masterUrl,
                    testModeOrNull);
        }
    }
}
