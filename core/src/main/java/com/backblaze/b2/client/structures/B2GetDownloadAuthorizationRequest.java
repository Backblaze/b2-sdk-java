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
    @B2Json.optional
    private final String b2ContentDisposition;

    @B2Json.constructor(params = "bucketId,fileNamePrefix,validDurationInSeconds,b2ContentDisposition")
    private B2GetDownloadAuthorizationRequest(String bucketId,
                                              String fileNamePrefix,
                                              int validDurationInSeconds,
                                              String b2ContentDisposition) {
        this.bucketId = bucketId;
        this.fileNamePrefix = fileNamePrefix;
        this.validDurationInSeconds = validDurationInSeconds;
        this.b2ContentDisposition = b2ContentDisposition;
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

    public String getB2ContentDisposition() {
        return b2ContentDisposition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2GetDownloadAuthorizationRequest that = (B2GetDownloadAuthorizationRequest) o;
        return validDurationInSeconds == that.validDurationInSeconds &&
                Objects.equals(bucketId, that.bucketId) &&
                Objects.equals(fileNamePrefix, that.fileNamePrefix) &&
                Objects.equals(b2ContentDisposition, that.b2ContentDisposition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketId, fileNamePrefix, validDurationInSeconds, b2ContentDisposition);
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
        private String b2ContentDisposition;


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
                    validDurationInSeconds,
                    b2ContentDisposition);
        }

        public Builder setB2ContentDisposition(String b2ContentDisposition) {
            this.b2ContentDisposition = b2ContentDisposition;
            return this;
        }
    }
}
