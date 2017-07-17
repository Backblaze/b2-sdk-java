/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2ContentSource;

import java.util.Objects;

public class B2UploadPartRequest {
    private final int partNumber;
    private final B2ContentSource contentSource;


    private B2UploadPartRequest(int partNumber,
                                B2ContentSource contentSource) {
        this.partNumber = partNumber;
        this.contentSource = contentSource;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public B2ContentSource getContentSource() {
        return contentSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2UploadPartRequest that = (B2UploadPartRequest) o;
        return getPartNumber() == that.getPartNumber() &&
                Objects.equals(getContentSource(), that.getContentSource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPartNumber(), getContentSource());
    }

    public static Builder builder(int partNumber,
                                  B2ContentSource source) {
        return new Builder(partNumber, source);
    }

    public static class Builder {
        private final int partNumber;
        private final B2ContentSource source;

        Builder(int partNumber,
                B2ContentSource source) {
            this.partNumber = partNumber;
            this.source = source;
        }

        public B2UploadPartRequest build() {
            return new B2UploadPartRequest(partNumber, source);
        }
    }
}
