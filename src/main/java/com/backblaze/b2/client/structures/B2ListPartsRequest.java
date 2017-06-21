/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2ListPartsRequest {
    @B2Json.required
    private final String fileId;
    @B2Json.optional
    private final Integer startPartNumber;
    @B2Json.optional
    private final Integer maxPartCount;

    @B2Json.constructor(params = "fileId,startPartNumber,maxPartCount")
    public B2ListPartsRequest(String fileId,
                              Integer startPartNumber,
                              Integer maxPartCount) {
        this.fileId = fileId;
        this.startPartNumber = startPartNumber;
        this.maxPartCount = maxPartCount;
    }

    public String getFileId() {
        return fileId;
    }

    public Integer getStartPartNumber() {
        return startPartNumber;
    }

    public Integer getMaxPartCount() {
        return maxPartCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2ListPartsRequest that = (B2ListPartsRequest) o;
        return Objects.equals(getFileId(), that.getFileId()) &&
                Objects.equals(getStartPartNumber(), that.getStartPartNumber()) &&
                Objects.equals(getMaxPartCount(), that.getMaxPartCount());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileId(), getStartPartNumber(), getMaxPartCount());
    }

    public static Builder builder(String largeFileId) {
        return new Builder(largeFileId);
    }

    public static Builder builder(B2ListPartsRequest origRequest) {
        return new Builder(origRequest);
    }

    public static class Builder {
        private final String fileId;
        private Integer startPartNumber;
        private Integer maxPartCount;

        public Builder(String fileId) {
            this.fileId = fileId;
        }

        public Builder(B2ListPartsRequest origRequest) {
            this(origRequest.fileId);
            setStartPartNumber(origRequest.startPartNumber);
            setMaxPartCount(origRequest.maxPartCount);
        }

        public B2ListPartsRequest build() {
            return new B2ListPartsRequest(
                    fileId,
                    startPartNumber,
                    maxPartCount);
        }

        public Builder setStartPartNumber(Integer startPartNumber) {
            this.startPartNumber = startPartNumber;
            return this;
        }

        public Builder setMaxPartCount(Integer maxPartCount) {
            this.maxPartCount = maxPartCount;
            return this;
        }
    }
}
