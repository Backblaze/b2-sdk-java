/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2GetUploadUrlRequest {
    @B2Json.required
    private final String bucketId;

    @B2Json.constructor(params = "bucketId")
    private B2GetUploadUrlRequest(String bucketId) {
        this.bucketId = bucketId;
    }

    public String getBucketId() {
        return bucketId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2GetUploadUrlRequest that = (B2GetUploadUrlRequest) o;
        return Objects.equals(getBucketId(), that.getBucketId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBucketId());
    }

    public static Builder builder(String bucketId) {
        return new Builder(bucketId);
    }

    public static class Builder {
        private final String bucketId;

        public Builder(String bucketId) {
            this.bucketId = bucketId;
        }

        public B2GetUploadUrlRequest build() {
            return new B2GetUploadUrlRequest(bucketId);
        }
    }
}