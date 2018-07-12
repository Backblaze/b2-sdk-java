/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;
import java.util.Set;

public class B2ListBucketsRequest {
    @B2Json.required
    private final String accountId;

    @B2Json.optional
    private final String bucketName;

    @B2Json.optional
    private final String bucketId;

    @B2Json.optional
    private final Set<String> bucketTypes;

    @B2Json.constructor(params = "accountId, bucketName, bucketId, bucketTypes")
    private B2ListBucketsRequest(String accountId, String bucketName, String bucketId, Set<String> bucketTypes) {
        B2Preconditions.checkArgumentIsNotNull(accountId, "accountId");
        this.accountId = accountId;
        this.bucketName = bucketName;
        this.bucketId = bucketId;
        this.bucketTypes = bucketTypes;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getBucketId() {
        return bucketId;
    }

    public Set<String> getBucketTypes() {
        return this.bucketTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2ListBucketsRequest that = (B2ListBucketsRequest) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(bucketName, that.bucketName) &&
                Objects.equals(bucketId, that.bucketId) &&
                Objects.equals(bucketTypes, that.bucketTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, bucketName, bucketId, bucketTypes);
    }

    public static Builder builder(String accountId) {
        return new Builder(accountId);
    }

    public static class Builder {
        private final String accountId;
        private String bucketName;
        private String bucketId;
        private Set<String> bucketTypes;

        public Builder(String accountId) {
            this.accountId = accountId;
        }

        public Builder setBucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public Builder setBucketId(String bucketId) {
            this.bucketId = bucketId;
            return this;
        }

        public Builder setBucketTypes(Set<String> bucketTypes) {
            this.bucketTypes = bucketTypes;
            return this;
        }

        public B2ListBucketsRequest build() {
            return new B2ListBucketsRequest(accountId, bucketName, bucketId, bucketTypes);
        }
    }
}
