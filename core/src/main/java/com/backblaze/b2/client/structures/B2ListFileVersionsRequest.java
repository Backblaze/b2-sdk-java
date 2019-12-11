/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

public class B2ListFileVersionsRequest {
    @B2Json.required
    private final String bucketId;
    @B2Json.optional
    private final String startFileName;
    @B2Json.optional
    private final String startFileId;
    @B2Json.optional
    private final Integer maxFileCount;
    @B2Json.optional
    private final String prefix;
    @B2Json.optional
    private final String delimiter;

    @B2Json.constructor(params = "bucketId,startFileName,startFileId,maxFileCount,prefix,delimiter")
    private B2ListFileVersionsRequest(String bucketId,
                                      String startFileName,
                                      String startFileId,
                                      Integer maxFileCount,
                                      String prefix,
                                      String delimiter) {
        this.bucketId = bucketId;
        this.startFileName = startFileName;
        this.startFileId = startFileId;
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

    public String getStartFileId() {
        return startFileId;
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
        B2ListFileVersionsRequest that = (B2ListFileVersionsRequest) o;
        return Objects.equals(getBucketId(), that.getBucketId()) &&
                Objects.equals(getStartFileName(), that.getStartFileName()) &&
                Objects.equals(getStartFileId(), that.getStartFileId()) &&
                Objects.equals(getMaxFileCount(), that.getMaxFileCount()) &&
                Objects.equals(getPrefix(), that.getPrefix()) &&
                Objects.equals(getDelimiter(), that.getDelimiter());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBucketId(), getStartFileName(), getStartFileId(), getMaxFileCount(), getPrefix(), getDelimiter());
    }

    public static Builder builder(String bucketId) {
        return new Builder(bucketId);
    }

    public static Builder builder(B2ListFileVersionsRequest origRequest) {
        return new Builder(origRequest);
    }

    public static class Builder {
        private final String bucketId;
        private String startFileName;
        private String startFileId;
        private Integer maxFileCount;
        private String prefix;
        private String delimiter;

        private Builder(String bucketId) {
            this.bucketId = bucketId;
        }

        public Builder(B2ListFileVersionsRequest orig) {
            this.bucketId = orig.bucketId;
            this.startFileName = orig.startFileName;
            this.startFileId = orig.startFileId;
            this.maxFileCount = orig.maxFileCount;
            this.prefix = orig.prefix;
            this.delimiter = orig.delimiter;
        }

        public B2ListFileVersionsRequest build() {
            return new B2ListFileVersionsRequest(
                    bucketId,
                    startFileName,
                    startFileId,
                    maxFileCount,
                    prefix,
                    delimiter);
        }

        public Builder setStart(String startFileName, String startFileId) {
            B2Preconditions.checkArgument(startFileName != null);
            // when "folders" are allowed to be returned from b2_list_file_versions,
            // sometimes we need to start queries from "folders", which have a fileName
            // but no fileId.  this means we can't require startFileId to be null.
            this.startFileName = startFileName;
            this.startFileId = startFileId;
            return this;
        }

        public Builder setStartFileName(String startFileName) {
            B2Preconditions.checkState(this.startFileName == null);
            this.startFileName = startFileName;
            return this;
        }

        public Builder setMaxFileCount(Integer maxFileCount) {
            this.maxFileCount = maxFileCount;
            return this;
        }

        public Builder setPrefix(String prefix) {
            B2Preconditions.checkState(this.prefix == null);
            this.prefix = prefix;
            return this;
        }

        @SuppressWarnings("SameParameterValue")
        public Builder setDelimiter(String delimiter) {
            B2Preconditions.checkState(this.delimiter == null);
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
