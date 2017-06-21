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

    public B2DownloadByIdRequest(String fileId,
                                 B2ByteRange range) {
        this.fileId = fileId;
        this.range = range;
    }

    public String getFileId() {
        return fileId;
    }

    public B2ByteRange getRange() {
        return range;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2DownloadByIdRequest that = (B2DownloadByIdRequest) o;
        return Objects.equals(getFileId(), that.getFileId()) &&
                Objects.equals(getRange(), that.getRange());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileId(), getRange());
    }

    public static Builder builder(String fileId) {
        return new Builder(fileId);
    }

    public static class Builder {
        private final String fileId;
        private B2ByteRange range;

        private Builder(String fileId) {
            this.fileId = fileId;
        }

        public Builder setRange(B2ByteRange range) {
            this.range = range;
            return this;
        }

        public B2DownloadByIdRequest build() {
            return new B2DownloadByIdRequest(fileId, range);
        }
    }
}
