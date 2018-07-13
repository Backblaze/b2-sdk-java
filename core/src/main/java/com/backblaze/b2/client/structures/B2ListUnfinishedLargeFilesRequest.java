/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

public class B2ListUnfinishedLargeFilesRequest {
    @B2Json.required
    private final String bucketId;
    @B2Json.optional
    private final String namePrefix;
    @B2Json.optional
    private final String startFileId;
    @B2Json.optional
    private final Integer maxFileCount;

    @B2Json.constructor(params = "bucketId,namePrefix,startFileId,maxFileCount")
    public B2ListUnfinishedLargeFilesRequest(String bucketId,
                                             String namePrefix,
                                             String startFileId,
                                             Integer maxFileCount) {
        B2Preconditions.checkArgumentIsNotNull(bucketId, "bucketId");
        this.bucketId = bucketId;
        this.namePrefix = namePrefix;
        this.startFileId = startFileId;
        this.maxFileCount = maxFileCount;
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public String getStartFileId() {
        return startFileId;
    }

    public Integer getMaxFileCount() {
        return maxFileCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2ListUnfinishedLargeFilesRequest that = (B2ListUnfinishedLargeFilesRequest) o;
        return Objects.equals(bucketId, that.bucketId) &&
                Objects.equals(namePrefix, that.namePrefix) &&
                Objects.equals(startFileId, that.startFileId) &&
                Objects.equals(maxFileCount, that.maxFileCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketId, namePrefix, startFileId, maxFileCount);
    }

    public static Builder builder(String bucketId) {
        return new Builder(bucketId);
    }

    public static Builder builder(B2ListUnfinishedLargeFilesRequest request) {
        return new Builder(request);
    }

    public static class Builder {
        private final String bucketId;
        private String namePrefix;
        private String startFileId;
        private Integer maxFileCount;

        public Builder(String bucketId) {
            this.bucketId = bucketId;
        }

        public Builder(B2ListUnfinishedLargeFilesRequest orig) {
            this.bucketId = orig.bucketId;
            this.namePrefix = orig.namePrefix;
            this.startFileId = orig.startFileId;
            this.maxFileCount = orig.maxFileCount;
        }

        public Builder setNamePrefix(String namePrefix) {
            this.namePrefix = namePrefix;
            return this;
        }

        public Builder setStartFileId(String startFileId) {
            this.startFileId = startFileId;
            return this;
        }

        public Builder setMaxFileCount(Integer maxFileCount) {
            this.maxFileCount = maxFileCount;
            return this;
        }

        public B2ListUnfinishedLargeFilesRequest build() {
            return new B2ListUnfinishedLargeFilesRequest(
                    bucketId,
                    namePrefix,
                    startFileId,
                    maxFileCount);
        }
    }
}
