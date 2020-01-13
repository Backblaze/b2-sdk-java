/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2GetDownloadAuthorizationRequest {
    @B2Json.required
    private final String bucketId;
    @B2Json.optional
    private final String fileNamePrefix;
    @B2Json.optional
    private final int validDurationInSeconds;
    @B2Json.optional
    private final String b2ContentDisposition;
    @B2Json.optional
    private final String b2ContentLanguage;
    @B2Json.optional
    private final String b2Expires;
    @B2Json.optional
    private final String b2CacheControl;
    @B2Json.optional
    private final String b2ContentEncoding;
    @B2Json.optional
    private final String b2ContentType;

    @B2Json.constructor(params = "bucketId,fileNamePrefix,validDurationInSeconds,b2ContentDisposition," +
                                 "b2ContentLanguage,b2Expires,b2CacheControl,b2ContentEncoding," +
                                 "b2ContentType")
    private B2GetDownloadAuthorizationRequest(String bucketId,
                                              String fileNamePrefix,
                                              int validDurationInSeconds,
                                              String b2ContentDisposition,
                                              String b2ContentLanguage,
                                              String b2Expires,
                                              String b2CacheControl,
                                              String b2ContentEncoding,
                                              String b2ContentType) {
        this.bucketId = bucketId;
        this.fileNamePrefix = fileNamePrefix;
        this.validDurationInSeconds = validDurationInSeconds;
        this.b2ContentDisposition = b2ContentDisposition;
        this.b2ContentLanguage = b2ContentLanguage;
        this.b2Expires = b2Expires;
        this.b2CacheControl = b2CacheControl;
        this.b2ContentEncoding = b2ContentEncoding;
        this.b2ContentType = b2ContentType;
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }

    public int getValidDurationInSeconds() {
        return validDurationInSeconds;
    }

    public String getB2ContentDisposition() {
        return b2ContentDisposition;
    }

    public String getB2ContentLanguage() {
        return b2ContentLanguage;
    }

    public String getB2Expires() {
        return b2Expires;
    }

    public String getB2CacheControl() {
        return b2CacheControl;
    }

    public String getB2ContentEncoding() {
        return b2ContentEncoding;
    }

    public String getB2ContentType() {
        return b2ContentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2GetDownloadAuthorizationRequest that = (B2GetDownloadAuthorizationRequest) o;
        return validDurationInSeconds == that.validDurationInSeconds &&
                Objects.equals(bucketId, that.bucketId) &&
                Objects.equals(fileNamePrefix, that.fileNamePrefix) &&
                Objects.equals(b2ContentDisposition, that.b2ContentDisposition) &&
                Objects.equals(b2ContentLanguage, that.b2ContentLanguage) &&
                Objects.equals(b2Expires, that.b2Expires) &&
                Objects.equals(b2CacheControl, that.b2CacheControl) &&
                Objects.equals(b2ContentEncoding, that.b2ContentEncoding) &&
                Objects.equals(b2ContentType, that.b2ContentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketId, fileNamePrefix, validDurationInSeconds, b2ContentDisposition, b2ContentLanguage, b2Expires, b2CacheControl, b2ContentEncoding, b2ContentType);
    }

    public static Builder builder(String bucketId,
                                  String fileNamePrefix,
                                  int validDurationInSeconds) {
        return new Builder(
                bucketId,
                fileNamePrefix,
                validDurationInSeconds);
    }

    public static class Builder {
        private final String bucketId;
        private final String fileNamePrefix;
        private final int validDurationInSeconds;
        private String b2ContentDisposition;
        private String b2ContentLanguage;
        private String b2Expires;
        private String b2CacheControl;
        private String b2ContentEncoding;
        private String b2ContentType;


        public Builder(String bucketId,
                       String fileNamePrefix,
                       int validDurationInSeconds) {
            this.bucketId = bucketId;
            this.fileNamePrefix = fileNamePrefix;
            this.validDurationInSeconds = validDurationInSeconds;
        }

        public B2GetDownloadAuthorizationRequest build() {
            return new B2GetDownloadAuthorizationRequest(
                    bucketId,
                    fileNamePrefix,
                    validDurationInSeconds,
                    b2ContentDisposition,
                    b2ContentLanguage,
                    b2Expires,
                    b2CacheControl,
                    b2ContentEncoding,
                    b2ContentType);
        }

        public Builder setB2ContentDisposition(String b2ContentDisposition) {
            this.b2ContentDisposition = b2ContentDisposition;
            return this;
        }

        public Builder setB2ContentLanguage(String b2ContentLanguage) {
            this.b2ContentLanguage = b2ContentLanguage;
            return this;
        }

        public Builder setB2Expires(String b2Expires) {
            this.b2Expires = b2Expires;
            return this;
        }

        public Builder setB2CacheControl(String b2CacheControl) {
            this.b2CacheControl = b2CacheControl;
            return this;
        }

        public Builder setB2ContentEncoding(String b2ContentEncoding) {
            this.b2ContentEncoding = b2ContentEncoding;
            return this;
        }

        public Builder setB2ContentType(String b2ContentType) {
            this.b2ContentType = b2ContentType;
            return this;
        }
    }
}
