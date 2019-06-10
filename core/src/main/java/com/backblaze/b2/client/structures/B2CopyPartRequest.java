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
    private final int partNumber;
    @B2Json.required
    private final String sourceFileId;
    @B2Json.required
    private final String largeFileId;
    @B2Json.optional
    private final String range;

    @B2Json.constructor(params = "partNumber, sourceFileId, largeFileId, range")
    private B2CopyPartRequest(int partNumber, String sourceFileId, String largeFileId, String range) {
        this.partNumber = partNumber;
        this.sourceFileId = sourceFileId;
        this.largeFileId = largeFileId;
        this.range = range;
    }

    public static Builder builder(int partNumber, String sourceFileId, String largeFileId) {
        return new Builder(partNumber, sourceFileId, largeFileId);
    }

    public int getPartNumber() {
        return partNumber;
    }

    public String getSourceFileId() {
        return sourceFileId;
    }

    public String getLargeFileId() {
        return largeFileId;
    }

    public String getRange() {
        return range;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2CopyPartRequest that = (B2CopyPartRequest) o;
        return partNumber == that.partNumber &&
                Objects.equals(sourceFileId, that.sourceFileId) &&
                Objects.equals(largeFileId, that.largeFileId) &&
                Objects.equals(range, that.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partNumber, sourceFileId, largeFileId, range);
    }

    public static class Builder {
        private final int partNumber;
        private final String sourceFileId;
        private final String largeFileId;
        private B2ByteRange range;

        public Builder(int partNumber, String sourceFileId, String largeFileId) {
            this.partNumber = partNumber;
            this.sourceFileId = sourceFileId;
            this.largeFileId = largeFileId;
        }

        public Builder setRange(B2ByteRange range) {
            this.range = range;
            return this;
        }

        public B2CopyPartRequest build() {
            return new B2CopyPartRequest(
                    partNumber, sourceFileId,
                    largeFileId,
                    range == null ? null : range.toString());
        }
    }
}

