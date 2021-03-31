/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Map;
import java.util.TreeMap;

public class B2UploadFileRequest {
    private final String bucketId;
    private final String fileName;
    private final String contentType;
    private final B2FileSseForRequest serverSideEncryption;
    private final B2FileRetention fileRetention;
    private final String legalHold;
    private final B2ContentSource contentSource;
    private final Map<String, String> fileInfo;
    private final B2UploadListener listener;


    private B2UploadFileRequest(String bucketId,
                                String fileName,
                                String contentType,
                                B2FileSseForRequest serverSideEncryption,
                                B2FileRetention fileRetention,
                                String legalHold,
                                Map<String, String> fileInfo,
                                B2ContentSource contentSource,
                                B2UploadListener listener) {
        this.bucketId = bucketId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.serverSideEncryption = serverSideEncryption;
        this.fileRetention = fileRetention;

        this.legalHold = legalHold;
        validateLegalHold(legalHold);

        this.fileInfo = fileInfo;  // make sorted, immutable copyOf?!
        this.contentSource = contentSource;
        this.listener = (listener != null) ? listener : B2UploadListener.noopListener();
    }

    private void validateLegalHold(String legalHold) {
        if (legalHold != null) {
            B2Preconditions.checkArgument(
                    legalHold.equals(B2LegalHold.ON) || legalHold.equals(B2LegalHold.OFF),
                    String.format("Invalid legalHold value. Valid values: %s, %s", B2LegalHold.ON, B2LegalHold.OFF));
        }
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public B2FileSseForRequest getServerSideEncryption() {
        return serverSideEncryption;
    }

    public B2FileRetention getFileRetention() {
        return fileRetention;
    }

    public String getLegalHold() {
        return legalHold;
    }

    public B2ContentSource getContentSource() {
        return contentSource;
    }

    public Map<String, String> getFileInfo() {
        return fileInfo;
    }

    public B2UploadListener getListener() {
        return listener;
    }

    public static Builder builder(String bucketId,
                                  String fileName,
                                  String contentType,
                                  B2ContentSource source) {
        return new Builder(bucketId, fileName, contentType, source);
    }

    public static class Builder {
        private String bucketId;
        private String fileName;
        private String contentType;
        private B2ContentSource source;
        private B2FileSseForRequest serverSideEncryption;
        private B2FileRetention fileRetention;
        private String legalHold;
        private Map<String, String> info;
        private B2UploadListener listener;

        Builder(String bucketId,
                String fileName,
                String contentType,
                B2ContentSource source) {
            this.bucketId = bucketId;
            this.fileName = fileName;
            this.contentType = contentType;
            this.source = source;
            this.info = new TreeMap<>();
        }

        public Builder setServerSideEncryption(B2FileSseForRequest serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
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

        public Builder setCustomField(String name, String value) {
            info.put(name, value);
            return this;
        }

        public Builder setCustomFields(Map<String, String> fileInfo) {
            if (fileInfo != null) {
                info.putAll(fileInfo);
            }
            return this;
        }

        public Builder setListener(B2UploadListener listener) {
            this.listener = listener;
            return this;
        }

        public B2UploadFileRequest build() {
            return new B2UploadFileRequest(bucketId,
                    fileName,
                    contentType,
                    serverSideEncryption,
                    fileRetention,
                    legalHold,
                    info,
                    source,
                    listener);
        }
    }
}
