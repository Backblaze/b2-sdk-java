/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import java.util.Objects;

public class B2DeleteBucketRequest {
    private final String bucketId;

    private B2DeleteBucketRequest(String bucketId) {
        this.bucketId = bucketId;
    }

    public static Builder builder(String bucketId) {
        return new Builder(bucketId);
    }

    public String getBucketId() {
        return bucketId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2DeleteBucketRequest that = (B2DeleteBucketRequest) o;
        return Objects.equals(getBucketId(), that.getBucketId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBucketId());
    }

    public static class Builder {
        private final String bucketId;

        public Builder(String bucketId) {
            this.bucketId = bucketId;
        }

        public B2DeleteBucketRequest build() {
            return new B2DeleteBucketRequest(bucketId);
        }
    }
}
