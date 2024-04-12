/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2GetBucketNotificationRulesRequest {

    @B2Json.required
    private final String bucketId;

    @B2Json.constructor
    private B2GetBucketNotificationRulesRequest(String bucketId) {
        this.bucketId = bucketId;
    }

    public String getBucketId() {
        return bucketId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2GetBucketNotificationRulesRequest that = (B2GetBucketNotificationRulesRequest) o;
        return bucketId.equals(that.bucketId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketId);
    }

    @Override
    public String toString() {
        return "B2GetBucketNotificationRulesRequest{" +
                "bucketId='" + bucketId + '\'' +
                '}';
    }

    public static B2GetBucketNotificationRulesRequest.Builder builder(String bucketId) {
        return new B2GetBucketNotificationRulesRequest.Builder(bucketId);
    }

    public static class Builder {
        private final String bucketId;

        public Builder(String bucketId) {
            this.bucketId = bucketId;
        }

        public B2GetBucketNotificationRulesRequest build() {
            return new B2GetBucketNotificationRulesRequest(bucketId);
        }
    }
}
