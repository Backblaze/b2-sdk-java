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

    @B2Json.required
    private final String sourceFileId;

    @B2Json.required
    private final String fileName;

    @B2Json.optional
    private final String range;

    @B2Json.optionalWithDefault(defaultValue = "\"COPY\"")
    private final MetadataDirective metadataDirective;

    @B2Json.optional
    private final String contentType;

    @B2Json.optional
    private final Map<String, String> fileInfo;

    @B2Json.constructor(params = "sourceFileId, fileName, range, metadataDirective, contentType, fileInfo")
    private B2CopyFileRequest(String sourceFileId, String fileName, String range, MetadataDirective metadataDirective, String contentType, Map<String, String> fileInfo) {
        this.sourceFileId = sourceFileId;
        this.fileName = fileName;
        this.range = range;
        this.metadataDirective = metadataDirective;
        this.contentType = contentType;
        this.fileInfo = fileInfo;
    }

    public static Builder builder(String sourceFileId, String fileName) {
        return new Builder(sourceFileId, fileName);
    }

    public String getSourceFileId() {
        return sourceFileId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getRange() {
        return range;
    }

    public MetadataDirective getMetadataDirective() {
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
        return Objects.equals(getSourceFileId(), that.getSourceFileId()) &&
                Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getRange(), that.getRange()) &&
                getMetadataDirective() == that.getMetadataDirective() &&
                Objects.equals(getContentType(), that.getContentType()) &&
                Objects.equals(getFileInfo(), that.getFileInfo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getSourceFileId(), getFileName(), getRange(), getMetadataDirective(), getContentType(), getFileInfo());
    }

    public static class Builder {
        private final String sourceFileId;
        private final String fileName;
        private B2ByteRange range;
        private MetadataDirective metadataDirective;
        private String contentType;
        private Map<String, String> fileInfo;

        public Builder(String sourceFileId, String fileName) {
            this.sourceFileId = sourceFileId;
            this.fileName = fileName;
        }

        public Builder setRange(B2ByteRange range) {
            this.range = range;
            return this;
        }

        public Builder setMetadataDirective(MetadataDirective metadataDirective) {
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
                    fileName,
                    range == null ? null : range.toString(),
                    metadataDirective,
                    contentType,
                    fileInfo);
        }
    }

}

