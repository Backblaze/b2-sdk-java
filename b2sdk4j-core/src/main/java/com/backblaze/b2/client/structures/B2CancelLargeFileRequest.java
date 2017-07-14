/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2CancelLargeFileRequest {
    @B2Json.required
    private final String fileId;

    @B2Json.constructor(params = "fileId")
    private B2CancelLargeFileRequest(String fileId) {
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2CancelLargeFileRequest request = (B2CancelLargeFileRequest) o;
        return Objects.equals(getFileId(), request.getFileId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileId());
    }

    public static Builder builder(String fileId) {
        return new Builder(fileId);
    }

    public static class Builder {
        private final String fileId;

        public Builder(String fileId) {
            this.fileId = fileId;
        }

        public B2CancelLargeFileRequest build() {
            return new B2CancelLargeFileRequest(fileId);
        }
    }
}
