/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.util.B2ByteRange;

import java.util.Objects;

public class B2DownloadByNameRequest {
    private final String bucketName;
    private final String fileName;
    private final B2ByteRange range;


    public B2DownloadByNameRequest(String bucketName,
                                   String fileName,
                                   B2ByteRange range) {
        // B2Preconditions.checkArg(bucketName != null);
        // B2Preconditions.checkArg(fileName != null);
        this.bucketName = bucketName;
        this.fileName = fileName;
        this.range = range;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getFileName() {
        return fileName;
    }

    public B2ByteRange getRange() {
        return range;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2DownloadByNameRequest that = (B2DownloadByNameRequest) o;
        return Objects.equals(getBucketName(), that.getBucketName()) &&
                Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getRange(), that.getRange());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBucketName(), getFileName(), getRange());
    }

    public static Builder builder(String bucketName,
                                  String fileName) {
        return new Builder(bucketName, fileName);
    }

    public static class Builder {
        private final String bucketName;
        private final String fileName;
        private B2ByteRange range;

        private Builder(String bucketName,
                       String fileName) {
            this.bucketName = bucketName;
            this.fileName = fileName;
        }

        public Builder setRange(B2ByteRange range) {
            this.range = range;
            return this;
        }

        public B2DownloadByNameRequest build() {
            return new B2DownloadByNameRequest(
                    bucketName,
                    fileName,
                    range);
        }
    }
}
