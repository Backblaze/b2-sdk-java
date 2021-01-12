/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

public class B2GetFileInfoByNameRequest {
    private final String bucketName;
    private final String fileName;
    private final String sseCustomerAlgorithm;
    private final String sseCustomerKey;
    private final String sseCustomerKeyMd5;

    public B2GetFileInfoByNameRequest(String bucketName,
                                      String fileName) {
        this(bucketName, fileName, null, null, null);
    }

    public B2GetFileInfoByNameRequest(String bucketName,
                                      String fileName,
                                      String sseCustomerAlgorithm,
                                      String sseCustomerKey,
                                      String sseCustomerKeyMd5) {
        B2Preconditions.checkArgument(bucketName != null);
        B2Preconditions.checkArgument(fileName != null);

        if (sseCustomerAlgorithm != null || sseCustomerKey != null || sseCustomerKeyMd5 != null) {
            B2Preconditions.checkArgument(sseCustomerAlgorithm != null);
            B2Preconditions.checkArgument(sseCustomerKey != null);
            B2Preconditions.checkArgument(sseCustomerKeyMd5 != null);
        }

        this.bucketName = bucketName;
        this.fileName = fileName;
        this.sseCustomerAlgorithm = sseCustomerAlgorithm;
        this.sseCustomerKey = sseCustomerKey;
        this.sseCustomerKeyMd5 = sseCustomerKeyMd5;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSseCustomerAlgorithmOrNull() {
        return sseCustomerAlgorithm;
    }

    public String getSseCustomerKeyOrNull() {
        return sseCustomerKey;
    }

    public String getSseCustomerKeyMd5OrNull() {
        return sseCustomerKeyMd5;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2GetFileInfoByNameRequest that = (B2GetFileInfoByNameRequest) o;
        return Objects.equals(bucketName, that.bucketName) &&
            Objects.equals(fileName, that.fileName) &&
            Objects.equals(sseCustomerAlgorithm, that.sseCustomerAlgorithm) &&
            Objects.equals(sseCustomerKey, that.sseCustomerKey) &&
            Objects.equals(sseCustomerKeyMd5, that.sseCustomerKeyMd5);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketName,
            fileName,
            sseCustomerAlgorithm,
            sseCustomerKey,
            sseCustomerKeyMd5);
    }

    public static Builder builder(String bucketName,
                                  String fileName) {
        return new Builder(bucketName, fileName);
    }

    public static class Builder {
        private final String bucketName;
        private final String fileName;

        private String sseCustomerAlgorithm;
        private String sseCustomerKey;
        private String sseCustomerKeyMd5;

        private Builder(String bucketName,
                        String fileName) {
            this.bucketName = bucketName;
            this.fileName = fileName;
        }

        public Builder setSseCustomerAlgorithm(String sseCustomerAlgorithm) {
            this.sseCustomerAlgorithm = sseCustomerAlgorithm;
            return this;
        }

        public Builder setSseCustomerKey(String sseCustomerKey) {
            this.sseCustomerKey = sseCustomerKey;
            return this;
        }

        public Builder setSseCustomerKeyMd5(String sseCustomerKeyMd5) {
            this.sseCustomerKeyMd5 = sseCustomerKeyMd5;
            return this;
        }

        public B2GetFileInfoByNameRequest build() {
            return new B2GetFileInfoByNameRequest(
                    bucketName,
                    fileName,
                    sseCustomerAlgorithm,
                    sseCustomerKey,
                    sseCustomerKeyMd5);
        }
    }
}
