/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

public class B2ListFileNamesRequest {
    @B2Json.required
    private final String bucketId;
    @B2Json.optional
    private final String startFileName;
    @B2Json.optional
    private final Integer maxFileCount;
    @B2Json.optional
    private final String prefix;
    @B2Json.optional
    private final String delimiter;

    @B2Json.constructor(params = "bucketId,startFileName,maxFileCount,prefix,delimiter")
    private B2ListFileNamesRequest(String bucketId,
                                   String startFileName,
                                   Integer maxFileCount,
                                   String prefix,
                                   String delimiter) {
        this.bucketId = bucketId;
        this.startFileName = startFileName;
        this.maxFileCount = maxFileCount;
        this.prefix = prefix;
        this.delimiter = delimiter;
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getStartFileName() {
        return startFileName;
    }

    public Integer getMaxFileCount() {
        return maxFileCount;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDelimiter() {
        return delimiter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2ListFileNamesRequest that = (B2ListFileNamesRequest) o;
        return Objects.equals(getBucketId(), that.getBucketId()) &&
                Objects.equals(getStartFileName(), that.getStartFileName()) &&
                Objects.equals(getMaxFileCount(), that.getMaxFileCount()) &&
                Objects.equals(getPrefix(), that.getPrefix()) &&
                Objects.equals(getDelimiter(), that.getDelimiter());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBucketId(), getStartFileName(), getMaxFileCount(), getPrefix(), getDelimiter());
    }

    public static Builder builder(String bucketId) {
        return new Builder(bucketId);
    }
    public static Builder builder(B2ListFileNamesRequest request) {
        return new Builder(request);
    }

    public static class Builder {
        private final String bucketId;
        private String startFileName;
        private Integer maxFileCount;
        private String prefix;
        private String delimiter;

        private Builder(String bucketId) {
            this.bucketId = bucketId;
        }

        public Builder(B2ListFileNamesRequest orig) {
            this.bucketId = orig.bucketId;
            this.startFileName = orig.startFileName;
            this.maxFileCount = orig.maxFileCount;
            this.prefix = orig.prefix;
            this.delimiter = orig.delimiter;
        }

        public B2ListFileNamesRequest build() {
            return new B2ListFileNamesRequest(
                    bucketId,
                    startFileName,
                    maxFileCount,
                    prefix,
                    delimiter);
        }

        public Builder setStartFileName(String startFileName) {
            this.startFileName = startFileName;
            return this;
        }

        public Builder setMaxFileCount(Integer maxFileCount) {
            this.maxFileCount = maxFileCount;
            return this;
        }

        public Builder setPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder setDelimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public Builder setWithinFolder(String withinFolder) {
            B2Preconditions.checkState(this.prefix == null);
            B2Preconditions.checkState(this.delimiter == null);
            this.prefix = withinFolder;
            this.delimiter = "/";
            return this;
        }
    }
}
