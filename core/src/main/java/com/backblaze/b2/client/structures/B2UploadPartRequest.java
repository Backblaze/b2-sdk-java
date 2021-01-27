/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

import static com.backblaze.b2.client.structures.B2ServerSideEncryptionMode.SSE_C;

public class B2UploadPartRequest {
    private final int partNumber;
    private final B2ContentSource contentSource;
    private final B2FileSseForRequest serverSideEncryption;


    private B2UploadPartRequest(int partNumber,
                                B2ContentSource contentSource,
                                B2FileSseForRequest serverSideEncryption) {
        // SSE parameters must be null for all but SSE-C part uploads
        B2Preconditions.checkArgument(serverSideEncryption == null || SSE_C.equals(serverSideEncryption.getMode()));

        this.partNumber = partNumber;
        this.contentSource = contentSource;
        this.serverSideEncryption = serverSideEncryption;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public B2ContentSource getContentSource() {
        return contentSource;
    }

    public B2FileSseForRequest getServerSideEncryption() {
        return serverSideEncryption;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2UploadPartRequest that = (B2UploadPartRequest) o;
        return getPartNumber() == that.getPartNumber() &&
                Objects.equals(getContentSource(), that.getContentSource()) &&
                Objects.equals(getServerSideEncryption(), that.getServerSideEncryption());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPartNumber(), getContentSource(), getServerSideEncryption());
    }

    public static Builder builder(int partNumber,
                                  B2ContentSource source) {
        return new Builder(partNumber, source);
    }

    public static class Builder {
        private final int partNumber;
        private final B2ContentSource source;
        private B2FileSseForRequest serverSideEncryption;

        Builder(int partNumber,
                B2ContentSource source) {
            this.partNumber = partNumber;
            this.source = source;
        }

        public Builder setServerSideEncryption(B2FileSseForRequest serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
            return this;
        }

        public B2UploadPartRequest build() {
            return new B2UploadPartRequest(partNumber, source, serverSideEncryption);
        }
    }
}
