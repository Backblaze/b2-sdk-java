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
    private final B2FileSseForRequest serverSideEncryption;

    public B2GetFileInfoByNameRequest(String bucketName,
                                      String fileName) {
        this(bucketName, fileName, null);
    }

    public B2GetFileInfoByNameRequest(String bucketName,
                                      String fileName,
                                      B2FileSseForRequest serverSideEncryption) {
        B2Preconditions.checkArgument(bucketName != null);
        B2Preconditions.checkArgument(fileName != null);

        this.bucketName = bucketName;
        this.fileName = fileName;
        this.serverSideEncryption = serverSideEncryption;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getFileName() {
        return fileName;
    }

    public B2FileSseForRequest getServerSideEncryption() {
        return serverSideEncryption;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2GetFileInfoByNameRequest that = (B2GetFileInfoByNameRequest) o;
        return Objects.equals(bucketName, that.bucketName) &&
            Objects.equals(fileName, that.fileName) &&
            Objects.equals(serverSideEncryption, that.serverSideEncryption);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketName,
            fileName,
            serverSideEncryption);
    }

    public static Builder builder(String bucketName,
                                  String fileName) {
        return new Builder(bucketName, fileName);
    }

    public static class Builder {
        private final String bucketName;
        private final String fileName;
        private B2FileSseForRequest serverSideEncryption;

        private Builder(String bucketName,
                        String fileName) {
            this.bucketName = bucketName;
            this.fileName = fileName;
        }

        public Builder setServerSideEncryption(B2FileSseForRequest serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
            return this;
        }

        public B2GetFileInfoByNameRequest build() {
            return new B2GetFileInfoByNameRequest(
                    bucketName,
                    fileName,
                    serverSideEncryption);
        }
    }
}
