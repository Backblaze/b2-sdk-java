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

    @B2Json.optional
    private final B2FileSseForRequest sourceServerSideEncryption;

    @B2Json.optional
    private final B2FileSseForRequest destinationServerSideEncryption;

    @B2Json.optional
    private final B2FileRetention fileRetention;

    @B2Json.optional
    private final String legalHold;

    @B2Json.constructor(params = "sourceFileId, destinationBucketId, fileName, range, metadataDirective, contentType, "+
            "fileInfo, sourceServerSideEncryption, destinationServerSideEncryption, " +
            "fileRetention, legalHold")
    private B2CopyFileRequest(
            String sourceFileId,
            String destinationBucketId,
            String fileName,
            String range,
            String metadataDirective,
            String contentType,
            Map<String, String> fileInfo,
            B2FileSseForRequest sourceServerSideEncryption,
            B2FileSseForRequest destinationServerSideEncryption,
            B2FileRetention fileRetention,
            String legalHold) {
        this.sourceFileId = sourceFileId;
        this.destinationBucketId = destinationBucketId;
        this.fileName = fileName;
        this.range = range;
        this.metadataDirective = metadataDirective;
        this.contentType = contentType;
        this.fileInfo = fileInfo;
        this.sourceServerSideEncryption = sourceServerSideEncryption;
        this.destinationServerSideEncryption = destinationServerSideEncryption;
        this.fileRetention = fileRetention;
        this.legalHold = legalHold;
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

    public B2FileSseForRequest getSourceServerSideEncryption() {
        return sourceServerSideEncryption;
    }

    public B2FileSseForRequest getDestinationServerSideEncryption() {
        return destinationServerSideEncryption;
    }

    public B2FileRetention getFileRetention() {
        return fileRetention;
    }

    public String getLegalHold() {
        return legalHold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2CopyFileRequest that = (B2CopyFileRequest) o;
        return Objects.equals(sourceFileId, that.sourceFileId) &&
                Objects.equals(destinationBucketId, that.destinationBucketId) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(range, that.range) &&
                Objects.equals(metadataDirective, that.metadataDirective) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(fileInfo, that.fileInfo) &&
                Objects.equals(sourceServerSideEncryption, that.sourceServerSideEncryption) &&
                Objects.equals(destinationServerSideEncryption, that.destinationServerSideEncryption) &&
                Objects.equals(fileRetention, that.fileRetention) &&
                Objects.equals(legalHold, that.legalHold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                sourceFileId,
                destinationBucketId,
                fileName,
                range,
                metadataDirective,
                contentType,
                fileInfo,
                sourceServerSideEncryption,
                destinationServerSideEncryption,
                fileRetention,
                legalHold
        );
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
        private B2FileSseForRequest sourceServerSideEncryption;
        private B2FileSseForRequest destinationServerSideEncryption;
        private B2FileRetention fileRetention;
        private String legalHold;

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

        public Builder setSourceServerSideEncryption(B2FileSseForRequest sourceServerSideEncryption) {
            this.sourceServerSideEncryption = sourceServerSideEncryption;
            return this;
        }

        public Builder setDestinationServerSideEncryption(B2FileSseForRequest destinationServerSideEncryption) {
            this.destinationServerSideEncryption = destinationServerSideEncryption;
            return this;
        }

        public Builder setFileRetention(B2FileRetention fileRetention) {
            this.fileRetention = fileRetention;
            return this;
        }

        public Builder setLegalHold(String legalHold) {
            this.legalHold = legalHold;
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
                    fileInfo,
                    sourceServerSideEncryption,
                    destinationServerSideEncryption,
                    fileRetention,
                    legalHold);
        }
    }

}

