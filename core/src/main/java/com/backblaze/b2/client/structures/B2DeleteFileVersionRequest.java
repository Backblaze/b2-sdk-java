/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2DeleteFileVersionRequest {
    @B2Json.required
    private final String fileName;
    @B2Json.required
    private final String fileId;

    @B2Json.optional
    private final boolean bypassGovernance;

    @B2Json.constructor(params = "fileName,fileId,bypassGovernance")
    public B2DeleteFileVersionRequest(String fileName,
                                      String fileId,
                                      boolean bypassGovernance) {
        this.fileName = fileName;
        this.fileId = fileId;
        this.bypassGovernance = bypassGovernance;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public boolean isBypassGovernance() {
        return bypassGovernance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2DeleteFileVersionRequest that = (B2DeleteFileVersionRequest) o;
        return Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getFileId(), that.getFileId()) &&
                isBypassGovernance() == that.isBypassGovernance();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileName(), getFileId(), isBypassGovernance());
    }

    public static Builder builder(String fileName,
                                  String fileId) {
        return new Builder(fileName, fileId);
    }

    public static class Builder {
        private final String fileName;
        private final String fileId;

        private boolean bypassGovernance;

        public Builder(String fileName,
                       String fileId) {
            this.fileName = fileName;
            this.fileId = fileId;
        }

        public Builder setBypassGovernance(boolean bypassGovernance) {
            this.bypassGovernance = bypassGovernance;
            return this;
        }

        public B2DeleteFileVersionRequest build() {
            return new B2DeleteFileVersionRequest(fileName, fileId, bypassGovernance);
        }
    }
}
