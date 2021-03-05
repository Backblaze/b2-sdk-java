/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Collections;
import com.backblaze.b2.util.B2Preconditions;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static com.backblaze.b2.client.contentSources.B2Headers.LARGE_FILE_SHA1_INFO_NAME;

public class B2StartLargeFileRequest {
    @B2Json.required
    private final String bucketId;
    @B2Json.required
    private final String fileName;
    @B2Json.required
    private final String contentType;
    @B2Json.optional
    private final B2FileSseForRequest serverSideEncryption;
    @B2Json.optional
    private final Map<String, String> fileInfo;

    @B2Json.optional
    private final B2FileRetention fileRetention;

    @B2Json.optional
    private final String legalHold;

    @B2Json.constructor(params = "bucketId,fileName,contentType,serverSideEncryption,fileInfo," +
            "fileRetention,legalHold")
    private B2StartLargeFileRequest(String bucketId,
                                    String fileName,
                                    String contentType,
                                    B2FileSseForRequest serverSideEncryption,
                                    Map<String, String> fileInfo,
                                    B2FileRetention fileRetention,
                                    String legalHold) {
        this.bucketId = bucketId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.serverSideEncryption = serverSideEncryption;
        this.fileInfo = B2Collections.unmodifiableMap(fileInfo);
        this.fileRetention = fileRetention;
        this.legalHold = legalHold;
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

    public Map<String, String> getFileInfo() {
        return fileInfo;
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
        final B2StartLargeFileRequest that = (B2StartLargeFileRequest) o;
        return Objects.equals(getBucketId(), that.getBucketId()) &&
                Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getContentType(), that.getContentType()) &&
                Objects.equals(getServerSideEncryption(), that.getServerSideEncryption()) &&
                Objects.equals(getFileInfo(), that.getFileInfo()) &&
                Objects.equals(getFileRetention(), that.getFileRetention()) &&
                Objects.equals(getLegalHold(), that.getLegalHold());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getBucketId(),
                getFileName(),
                getContentType(),
                getServerSideEncryption(),
                getFileInfo(),
                getFileRetention(),
                getLegalHold()
        );
    }

    /**
     * This is only public so it is available from other sdk packages.
     * Consider it internal.
     */
    public static B2StartLargeFileRequest buildFrom(B2UploadFileRequest orig) throws B2Exception {
        try {
            final Builder builder = new Builder(orig.getBucketId(), orig.getFileName(), orig.getContentType());

            // copy SSE settings (if any) from original
            builder.setServerSideEncryption(orig.getServerSideEncryption());

            // copy file retention settings (if any) from original
            builder.setFileRetention(orig.getFileRetention());

            // copy legal hold (if any) from original
            builder.setLegalHold(orig.getLegalHold());

            // we always start with the original fileInfo.
            builder.setCustomFields(orig.getFileInfo());

            final String largeFileSha1 = orig.getContentSource().getSha1OrNull();
            if (largeFileSha1 != null) {
                // there's a largeFileSha1 in the contentSource, so use it.

                // if there already was one in the request, make sure it matches.
                final String origLargeFileSha1 = orig.getFileInfo().get(LARGE_FILE_SHA1_INFO_NAME);
                B2Preconditions.checkArgument(
                        origLargeFileSha1 == null ||
                                Objects.equals(largeFileSha1, origLargeFileSha1),
                        "already have largeFileSha1 and it doesn't match?");

                builder.setCustomField(LARGE_FILE_SHA1_INFO_NAME, largeFileSha1);
            }

            return builder.build();
        } catch (IOException e) {
            throw new B2LocalException("local", "failed to get large file's sha1 from contentSource: " + e.getMessage(), e);
        }
    }


    public static Builder builder(String bucketId,
                                  String fileName,
                                  String contentType) {
        return new Builder(bucketId, fileName, contentType);
    }

    public static class Builder {
        private String bucketId;
        private String fileName;
        private String contentType;
        private B2FileSseForRequest serverSideEncryption;
        private Map<String, String> fileInfo;
        private B2FileRetention fileRetention;
        private String legalHold;

        Builder(String bucketId,
                String fileName,
                String contentType) {
            this.bucketId = bucketId;
            this.fileName = fileName;
            this.contentType = contentType;
            this.fileInfo = new TreeMap<>();
        }

        /**
         * Sets one of your custom fields to be this well-known field.
         * @param lastModifiedMillis the time the "source" of this file was last modified.
         */
        public Builder setSrcLastModifiedMillisOrNull(long lastModifiedMillis) {
            setCustomField("src_last_modified_millis", Long.toString(lastModifiedMillis));
            return this;
        }

        public Builder setCustomField(String name, String value) {
            B2Preconditions.checkArgumentIsNotNull(value, "value");
            fileInfo.put(name, value);
            return this;
        }

        public Builder setLargeFileSha1(String largeFileSha1) {
            return setCustomField("large_file_sha1", largeFileSha1);
        }

        public Builder setServerSideEncryption(B2FileSseForRequest serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
            return this;
        }

        public Builder setCustomFields(Map<String,String> newFileInfo) {
            B2Preconditions.checkArgumentIsNotNull(newFileInfo, "newFileInfo");
            newFileInfo.forEach(this::setCustomField);
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

        public B2StartLargeFileRequest build() {
            return new B2StartLargeFileRequest(bucketId,
                    fileName,
                    contentType,
                    serverSideEncryption,
                    fileInfo,
                    fileRetention,
                    legalHold);
        }
    }
}
