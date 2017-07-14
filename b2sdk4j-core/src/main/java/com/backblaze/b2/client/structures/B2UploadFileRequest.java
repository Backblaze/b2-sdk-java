/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2ContentSource;

import java.util.Map;
import java.util.TreeMap;

public class B2UploadFileRequest {
    private final String bucketId;
    private final String fileName;
    private final String contentType;
    private final B2ContentSource contentSource;
    private final Map<String, String> fileInfo;


    private B2UploadFileRequest(String bucketId,
                                String fileName,
                                String contentType,
                                Map<String, String> fileInfo,
                                B2ContentSource contentSource) {
        this.bucketId = bucketId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileInfo = fileInfo;  // make sorted, immutable copyOf?!
        this.contentSource = contentSource;
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

    public B2ContentSource getContentSource() {
        return contentSource;
    }

    public Map<String, String> getFileInfo() {
        return fileInfo;
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
        private Map<String, String> info;

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

        public B2UploadFileRequest build() {
            return new B2UploadFileRequest(bucketId,
                    fileName,
                    contentType,
                    info,
                    source);
        }
    }
}
