/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2GetDownloadAuthorizationRequest {
    @B2Json.required
    private final String bucketId;
    @B2Json.optional
    private final String fileNamePrefix;
    @B2Json.optional
    private final int validDurationInSeconds;

    @B2Json.constructor(params = "bucketId,fileNamePrefix,validDurationInSeconds")
    private B2GetDownloadAuthorizationRequest(String bucketId,
                                              String fileNamePrefix,
                                              int validDurationInSeconds) {
        this.bucketId = bucketId;
        this.fileNamePrefix = fileNamePrefix;
        this.validDurationInSeconds = validDurationInSeconds;
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }

    public int getValidDurationInSeconds() {
        return validDurationInSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2GetDownloadAuthorizationRequest that = (B2GetDownloadAuthorizationRequest) o;
        return getValidDurationInSeconds() == that.getValidDurationInSeconds() &&
                Objects.equals(getBucketId(), that.getBucketId()) &&
                Objects.equals(getFileNamePrefix(), that.getFileNamePrefix());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBucketId(), getFileNamePrefix(), getValidDurationInSeconds());
    }

    public static Builder builder(String bucketId,
                                  String fileNamePrefix,
                                  int validDurationInSeconds) {
        return new Builder(
                bucketId,
                fileNamePrefix,
                validDurationInSeconds);
    }

    public static class Builder {
        private final String bucketId;
        private final String fileNamePrefix;
        private final int validDurationInSeconds;


        public Builder(String bucketId,
                       String fileNamePrefix,
                       int validDurationInSeconds) {
            this.bucketId = bucketId;
            this.fileNamePrefix = fileNamePrefix;
            this.validDurationInSeconds = validDurationInSeconds;
        }

        public B2GetDownloadAuthorizationRequest build() {
            return new B2GetDownloadAuthorizationRequest(
                    bucketId,
                    fileNamePrefix,
                    validDurationInSeconds);
        }
    }
}
