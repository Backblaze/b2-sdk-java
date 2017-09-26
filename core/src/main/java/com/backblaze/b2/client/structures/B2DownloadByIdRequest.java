/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.util.B2ByteRange;

import java.util.Objects;

public class B2DownloadByIdRequest {
    private final String fileId;
    private final B2ByteRange range;
    private final String b2ContentDisposition;

    public B2DownloadByIdRequest(String fileId,
                                 B2ByteRange range,
                                 String b2ContentDisposition) {
        this.fileId = fileId;
        this.range = range;
        this.b2ContentDisposition = b2ContentDisposition;
    }

    public String getFileId() {
        return fileId;
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
        B2DownloadByIdRequest that = (B2DownloadByIdRequest) o;
        return Objects.equals(fileId, that.fileId) &&
                Objects.equals(range, that.range) &&
                Objects.equals(b2ContentDisposition, that.b2ContentDisposition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, range, b2ContentDisposition);
    }

    public static Builder builder(String fileId) {
        return new Builder(fileId);
    }

    public static class Builder {
        private final String fileId;
        private B2ByteRange range;
        private String b2ContentDisposition;

        private Builder(String fileId) {
            this.fileId = fileId;
        }

        public Builder setRange(B2ByteRange range) {
            this.range = range;
            return this;
        }

        public B2DownloadByIdRequest build() {
            return new B2DownloadByIdRequest(fileId, range, b2ContentDisposition);
        }

        @SuppressWarnings("unused")
        public Builder setB2ContentDisposition(String b2ContentDisposition) {
            this.b2ContentDisposition = b2ContentDisposition;
            return this;
        }
    }
}
