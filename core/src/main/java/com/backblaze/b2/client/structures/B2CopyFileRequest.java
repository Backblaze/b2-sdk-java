/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;


import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2ByteRange;

import java.util.Map;
import java.util.Objects;

public class B2CopyFileRequest {

    public static final String COPY_METADATA_DIRECTIVE = "COPY";
    public static final String REPLACE_METADATA_DIRECTIVE = "REPLACE";

    @B2Json.required
    private final String sourceFileId;

    @B2Json.optional
    private final String destinationBucketId;

    @B2Json.required
    private final String fileName;

    @B2Json.optional
    private final String range;

    @B2Json.optionalWithDefault(defaultValue = "\"COPY\"")
    private final String metadataDirective;

    @B2Json.optional
    private final String contentType;

    @B2Json.optional
    private final Map<String, String> fileInfo;

    @B2Json.constructor(params = "sourceFileId, destinationBucketId, fileName, range, metadataDirective, contentType, fileInfo")
    private B2CopyFileRequest(
            String sourceFileId,
            String destinationBucketId,
            String fileName,
            String range,
            String metadataDirective,
            String contentType,
            Map<String, String> fileInfo) {

        this.sourceFileId = sourceFileId;
        this.destinationBucketId = destinationBucketId;
        this.fileName = fileName;
        this.range = range;
        this.metadataDirective = metadataDirective;
        this.contentType = contentType;
        this.fileInfo = fileInfo;
    }

    public String getSourceFileId() {
        return sourceFileId;
    }

    public String getDestinationBucketId() {
        return destinationBucketId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getRange() {
        return range;
    }

    public String getMetadataDirective() {
        return metadataDirective;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, String> getFileInfo() {
        return fileInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2CopyFileRequest that = (B2CopyFileRequest) o;
        return Objects.equals(sourceFileId, that.sourceFileId) &&
                Objects.equals(destinationBucketId, that.destinationBucketId) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(range, that.range) &&
                metadataDirective == that.metadataDirective &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(fileInfo, that.fileInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceFileId, destinationBucketId, fileName, range, metadataDirective, contentType, fileInfo);
    }

    public static Builder builder(String sourceFileId, String fileName) {
        return new Builder(sourceFileId, fileName);
    }

    public static class Builder {
        private final String sourceFileId;
        private String destinationBucketId;
        private final String fileName;
        private B2ByteRange range;
        private String metadataDirective;
        private String contentType;
        private Map<String, String> fileInfo;

        public Builder(String sourceFileId, String fileName) {
            this.sourceFileId = sourceFileId;
            this.fileName = fileName;
        }

        public Builder setDestinationBucketId(String destinationBucketId) {
            this.destinationBucketId = destinationBucketId;
            return this;
        }

        public Builder setRange(B2ByteRange range) {
            this.range = range;
            return this;
        }

        public Builder setMetadataDirective(String metadataDirective) {
            this.metadataDirective = metadataDirective;
            return this;
        }

        public Builder setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder setFileInfo(Map<String, String> fileInfo) {
            this.fileInfo = fileInfo;
            return this;
        }

        public B2CopyFileRequest build() {
            return new B2CopyFileRequest(
                    sourceFileId,
                    destinationBucketId,
                    fileName,
                    range == null ? null : range.toString(),
                    metadataDirective,
                    contentType,
                    fileInfo);
        }
    }

}

