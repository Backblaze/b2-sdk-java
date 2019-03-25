/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

/**
 *
 * This structure holds the information for b2_authorize_account.
 *
 * If you used getAccountId() in a previous version of the SDK, please
 * use B2StorageClient.getAccountId() in this version.
 */
public class B2AuthorizeAccountRequest {
    @B2Json.required
    private final String applicationKeyId;
    @B2Json.required
    private final String applicationKey;

    @B2Json.constructor(params = "applicationKeyId,applicationKey")
    public B2AuthorizeAccountRequest(String applicationKeyId,
                                     String applicationKey) {
        this.applicationKeyId = applicationKeyId;
        this.applicationKey = applicationKey;
    }

    public static Builder builder(String applicationKeyId,
                                  String applicationKey) {
        return new Builder(applicationKeyId, applicationKey);

    }

    public String getApplicationKeyId() {
        return applicationKeyId;
    }

    public String getApplicationKey() {
        return applicationKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2AuthorizeAccountRequest that = (B2AuthorizeAccountRequest) o;
        return Objects.equals(getApplicationKeyId(), that.getApplicationKeyId()) &&
                Objects.equals(getApplicationKey(), that.getApplicationKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getApplicationKeyId(), getApplicationKey());
    }

    public static class Builder {
        private final String applicationKeyId;
        private final String applicationKey;

        public Builder(String applicationKeyId,
                       String applicationKey) {
            this.applicationKeyId = applicationKeyId;
            this.applicationKey = applicationKey;
        }

        public B2AuthorizeAccountRequest build() {
            return new B2AuthorizeAccountRequest(applicationKeyId, applicationKey);
        }
    }
}
