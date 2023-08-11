/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

import static com.backblaze.b2.client.structures.B2ServerSideEncryptionMode.SSE_C;

public class B2StoreLargeFileRequest {
    @B2Json.optional
    private final String fileId;
    @B2Json.required
    private final B2FileVersion b2FileVersion;
    @B2Json.optional
    private final B2FileSseForRequest serverSideEncryption;

    @B2Json.constructor(params = "fileId,fileVersion,serverSideEncryption")
    private B2StoreLargeFileRequest(String fileId,
                                    B2FileVersion b2FileVersion,
                                    B2FileSseForRequest serverSideEncryption) {
        B2Preconditions.checkArgumentIsNotNull(b2FileVersion, "b2FileVersion");
        // SSE parameters must be null for all but SSE-C part uploads
        B2Preconditions.checkArgument(serverSideEncryption == null || SSE_C.equals(serverSideEncryption.getMode()));

        this.fileId = (fileId != null) ? fileId : b2FileVersion.getFileId();
        this.b2FileVersion = b2FileVersion;
        this.serverSideEncryption = serverSideEncryption;
    }

    public String getFileId() {
        return fileId;
    }

    @Deprecated
    public B2FileVersion getFileVersion() {
        return b2FileVersion;
    }

    public B2FileSseForRequest getServerSideEncryption() {
        return serverSideEncryption;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2StoreLargeFileRequest that = (B2StoreLargeFileRequest) o;
        return Objects.equals(getFileId(), that.getFileId()) &&
                Objects.equals(getServerSideEncryption(), that.getServerSideEncryption());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileId(), getServerSideEncryption());
    }

    @Deprecated
    public static Builder builder(B2FileVersion fileVersion) {
        return new Builder(fileVersion);
    }

    public static Builder builder(String fileId) {
        return new Builder(fileId);
    }

    public static class Builder {
        private final String fileId;
        private final B2FileVersion b2FileVersion;
        private B2FileSseForRequest serverSideEncryption;

        Builder(String fileId) {
            this.fileId = fileId;
            // old B2LargeFileStorer code only uses the file ID, so we're deprecating the b2FileVersion in favor
            // of just using the file ID directly; in the meantime, we create a b2FileVersion that only has the
            // fileId field and everything else set to null/zero
            this.b2FileVersion = new B2FileVersion(
                    fileId,
                    null,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    null,
                    null
            );
        }

        Builder(B2FileVersion b2FileVersion) {
            this.b2FileVersion = b2FileVersion;
            this.fileId = b2FileVersion.getFileId();
        }


        public Builder setServerSideEncryption(B2FileSseForRequest serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
            return this;
        }

        public B2StoreLargeFileRequest build() {
            return new B2StoreLargeFileRequest(fileId, b2FileVersion, serverSideEncryption);
        }
    }
}
