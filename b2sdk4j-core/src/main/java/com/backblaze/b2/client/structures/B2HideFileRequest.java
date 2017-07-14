/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2HideFileRequest {
    @B2Json.required
    private final String bucketId;
    @B2Json.required
    private final String fileName;

    @B2Json.constructor(params = "bucketId,fileName")
    private B2HideFileRequest(String bucketId,
                             String fileName) {
        this.bucketId = bucketId;
        this.fileName = fileName;
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2HideFileRequest that = (B2HideFileRequest) o;
        return Objects.equals(getBucketId(), that.getBucketId()) &&
                Objects.equals(getFileName(), that.getFileName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBucketId(), getFileName());
    }

    public static Builder builder(String bucketId,
                                  String fileName) {
        return new Builder(bucketId, fileName);
    }

    public static class Builder {
        private final String bucketId;
        private final String fileName;

        public Builder(String bucketId,
                       String fileName) {
            this.bucketId = bucketId;
            this.fileName = fileName;
        }

        public B2HideFileRequest build() {
            return new B2HideFileRequest(bucketId, fileName);
        }
    }
}
