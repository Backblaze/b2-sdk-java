/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;
import java.util.Set;

public class B2ListBucketsRequest {
    @B2Json.required
    private final String accountId;

    @B2Json.optional
    private final Set<String> bucketTypes;

    @B2Json.constructor(params = "accountId, bucketTypes")
    private B2ListBucketsRequest(String accountId, Set<String> bucketTypes) {
        this.accountId = accountId;
        this.bucketTypes = bucketTypes;
    }

    public Set<String> getBucketTypes() {
        return this.bucketTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2ListBucketsRequest that = (B2ListBucketsRequest) o;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(bucketTypes, that.bucketTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, bucketTypes);
    }

    public static Builder builder(String accountId) {
        return new Builder(accountId);
    }

    public static class Builder {
        private final String accountId;
        private Set<String> bucketTypes;
        public Builder(String accountId) {
            this.accountId = accountId;
        }

        public Builder setBucketTypes(Set<String> bucketTypes) {
            this.bucketTypes = bucketTypes;
            return this;
        }

        public B2ListBucketsRequest build() {
            return new B2ListBucketsRequest(accountId, bucketTypes);
        }
    }
}
