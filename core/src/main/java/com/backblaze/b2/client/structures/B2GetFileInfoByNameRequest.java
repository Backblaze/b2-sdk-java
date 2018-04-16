/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import java.util.Objects;

public class B2GetFileInfoByNameRequest {
    private final String bucketName;
    private final String fileName;

    public B2GetFileInfoByNameRequest(String bucketName,
                                      String fileName) {
        // B2Preconditions.checkArg(bucketName != null);
        // B2Preconditions.checkArg(fileName != null);
        this.bucketName = bucketName;
        this.fileName = fileName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2GetFileInfoByNameRequest that = (B2GetFileInfoByNameRequest) o;
        return Objects.equals(bucketName, that.bucketName) &&
                Objects.equals(fileName, that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketName, fileName);
    }

    public static Builder builder(String bucketName,
                                  String fileName) {
        return new Builder(bucketName, fileName);
    }

    public static class Builder {
        private final String bucketName;
        private final String fileName;

        private Builder(String bucketName,
                       String fileName) {
            this.bucketName = bucketName;
            this.fileName = fileName;
        }

        public B2GetFileInfoByNameRequest build() {
            return new B2GetFileInfoByNameRequest(
                    bucketName,
                    fileName);
        }
    }
}
