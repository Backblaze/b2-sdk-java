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
    @B2Json.required
    private final B2FileVersion fileVersion;
    @B2Json.optional
    private final B2FileSseForRequest serverSideEncryption;

    @B2Json.constructor(params = "fileVersion,serverSideEncryption")
    private B2StoreLargeFileRequest(B2FileVersion fileVersion,
                                    B2FileSseForRequest serverSideEncryption) {
        B2Preconditions.checkArgumentIsNotNull(fileVersion, "fileVersion");
        // SSE parameters must be null for all but SSE-C part uploads
        B2Preconditions.checkArgument(serverSideEncryption == null || SSE_C.equals(serverSideEncryption.getMode()));

        this.fileVersion = fileVersion;
        this.serverSideEncryption = serverSideEncryption;
    }

    public B2FileVersion getFileVersion() {
        return fileVersion;
    }

    public B2FileSseForRequest getServerSideEncryption() {
        return serverSideEncryption;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2StoreLargeFileRequest that = (B2StoreLargeFileRequest) o;
        return Objects.equals(getFileVersion(), that.getFileVersion()) &&
                Objects.equals(getServerSideEncryption(), that.getServerSideEncryption());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileVersion(), getServerSideEncryption());
    }

    public static Builder builder(B2FileVersion fileVersion) {
        return new Builder(fileVersion);
    }

    public static class Builder {
        private B2FileVersion fileVersion;
        private B2FileSseForRequest serverSideEncryption;

        Builder(B2FileVersion fileVersion) {
            this.fileVersion = fileVersion;
        }

        public Builder setServerSideEncryption(B2FileSseForRequest serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
            return this;
        }

        public B2StoreLargeFileRequest build() {
            return new B2StoreLargeFileRequest(fileVersion, serverSideEncryption);
        }
    }
}
