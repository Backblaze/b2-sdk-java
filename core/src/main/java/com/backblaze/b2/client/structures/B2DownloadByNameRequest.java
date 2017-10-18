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
    private final String b2ContentDisposition;


    public B2DownloadByNameRequest(String bucketName,
                                   String fileName,
                                   B2ByteRange range,
                                   String b2ContentDisposition) {
        // B2Preconditions.checkArg(bucketName != null);
        // B2Preconditions.checkArg(fileName != null);
        this.bucketName = bucketName;
        this.fileName = fileName;
        this.range = range;
        this.b2ContentDisposition = b2ContentDisposition;
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

    public String getB2ContentDisposition() {
        return b2ContentDisposition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2DownloadByNameRequest that = (B2DownloadByNameRequest) o;
        return Objects.equals(bucketName, that.bucketName) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(range, that.range) &&
                Objects.equals(b2ContentDisposition, that.b2ContentDisposition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketName, fileName, range, b2ContentDisposition);
    }

    public static Builder builder(String bucketName,
                                  String fileName) {
        return new Builder(bucketName, fileName);
    }

    public static class Builder {
        private final String bucketName;
        private final String fileName;
        private B2ByteRange range;
        private String b2ContentDisposition;

        private Builder(String bucketName,
                       String fileName) {
            this.bucketName = bucketName;
            this.fileName = fileName;
        }

        public Builder setRange(B2ByteRange range) {
            this.range = range;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder setB2ContentDisposition(String b2ContentDisposition) {
            this.b2ContentDisposition = b2ContentDisposition;
            return this;
        }


        public B2DownloadByNameRequest build() {
            return new B2DownloadByNameRequest(
                    bucketName,
                    fileName,
                    range, b2ContentDisposition);
        }
    }
}
