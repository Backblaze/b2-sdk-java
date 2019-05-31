/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2ByteRange;

import java.util.Objects;

public class B2CopyPartRequest {

    @B2Json.required
    private final String sourceFileId;
    @B2Json.required
    private final String largeFileId;
    @B2Json.required
    private final int partNumber;
    @B2Json.optional
    private final String range;

    @B2Json.constructor(params = "sourceFileId, largeFileId, partNumber, range")
    private B2CopyPartRequest(String sourceFileId, String largeFileId, int partNumber, String range) {
        this.sourceFileId = sourceFileId;
        this.largeFileId = largeFileId;
        this.partNumber = partNumber;
        this.range = range;
    }

    public static Builder builder(String sourceFileId, String largeFileId, int partNumber) {
        return new Builder(sourceFileId, largeFileId, partNumber);
    }

    public String getSourceFileId() {
        return sourceFileId;
    }

    public String getLargeFileId() {
        return largeFileId;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public String getRange() {
        return range;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2CopyPartRequest that = (B2CopyPartRequest) o;
        return getPartNumber() == that.getPartNumber() &&
                Objects.equals(getSourceFileId(), that.getSourceFileId()) &&
                Objects.equals(getLargeFileId(), that.getLargeFileId()) &&
                Objects.equals(getRange(), that.getRange());
    }

    public static class Builder {
        private final String sourceFileId;
        private final String largeFileId;
        private final int partNumber;
        private B2ByteRange range;

        public Builder(String sourceFileId, String largeFileId, int partNumber) {
            this.sourceFileId = sourceFileId;
            this.largeFileId = largeFileId;
            this.partNumber = partNumber;
        }

        public Builder setRange(B2ByteRange range) {
            this.range = range;
            return this;
        }

        public B2CopyPartRequest build() {
            return new B2CopyPartRequest(
                    sourceFileId,
                    largeFileId,
                    partNumber,
                    range == null ? null : range.toString());
        }
    }
}

