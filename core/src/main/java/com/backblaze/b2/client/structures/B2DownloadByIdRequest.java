/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.util.B2ByteRange;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class B2DownloadByIdRequest implements B2OverrideableHeaders {
    private final String fileId;
    private final B2ByteRange range;
    private final B2FileSseForRequest serverSideEncryption;
    private final String b2ContentDisposition;
    private final String b2ContentLanguage;
    private final String b2Expires;
    private final String b2CacheControl;
    private final String b2ContentEncoding;
    private final String b2ContentType;

    private B2DownloadByIdRequest(String fileId,
                                  B2ByteRange range,
                                  B2FileSseForRequest serverSideEncryption,
                                  String b2ContentDisposition,
                                  String b2ContentLanguage,
                                  String b2Expires,
                                  String b2CacheControl,
                                  String b2ContentEncoding,
                                  String b2ContentType) {

        this.fileId = fileId;
        this.range = range;
        this.serverSideEncryption = serverSideEncryption;
        this.b2ContentDisposition = b2ContentDisposition;
        this.b2ContentLanguage = b2ContentLanguage;
        this.b2Expires = b2Expires;
        this.b2CacheControl = b2CacheControl;
        this.b2ContentEncoding = b2ContentEncoding;
        this.b2ContentType = b2ContentType;
    }

    public String getFileId() {
        return fileId;
    }

    public B2ByteRange getRange() {
        return range;
    }

    public B2FileSseForRequest getServerSideEncryption() {
        return serverSideEncryption;
    }

    @Override
    public String getB2ContentDisposition() {
        return b2ContentDisposition;
    }

    @Override
    public String getB2ContentLanguage() {
        return b2ContentLanguage;
    }

    @Override
    public String getB2Expires() {
        return b2Expires;
    }

    @Override
    public String getB2CacheControl() {
        return b2CacheControl;
    }

    @Override
    public String getB2ContentEncoding() {
        return b2ContentEncoding;
    }

    @Override
    public String getB2ContentType() {
        return b2ContentType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2DownloadByIdRequest that = (B2DownloadByIdRequest) o;
        return Objects.equals(fileId, that.fileId) &&
                Objects.equals(range, that.range) &&
                Objects.equals(serverSideEncryption, that.serverSideEncryption) &&
                Objects.equals(b2ContentDisposition, that.b2ContentDisposition) &&
                Objects.equals(b2ContentLanguage, that.b2ContentLanguage) &&
                Objects.equals(b2Expires, that.b2Expires) &&
                Objects.equals(b2CacheControl, that.b2CacheControl) &&
                Objects.equals(b2ContentEncoding, that.b2ContentEncoding) &&
                Objects.equals(b2ContentType, that.b2ContentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, range, serverSideEncryption, b2ContentDisposition, b2ContentLanguage, b2Expires, b2CacheControl, b2ContentEncoding, b2ContentType);
    }

    public static Builder builder(String fileId) {
        return new Builder(fileId);
    }

    public static class Builder {
        private final String fileId;
        private B2ByteRange range;
        private B2FileSseForRequest serverSideEncryption;
        private String b2ContentDisposition;
        private String b2ContentLanguage;
        private String b2Expires;
        private String b2CacheControl;
        private String b2ContentEncoding;
        private String b2ContentType;

        private Builder(String fileId) {
            this.fileId = fileId;
        }

        public B2DownloadByIdRequest build() {
            return new B2DownloadByIdRequest(fileId,
                    range,
                    serverSideEncryption,
                    b2ContentDisposition,
                    b2ContentLanguage,
                    b2Expires,
                    b2CacheControl,
                    b2ContentEncoding,
                    b2ContentType);
        }

        public Builder setRange(B2ByteRange range) {
            this.range = range;
            return this;
        }

        public Builder setServerSideEncryption(B2FileSseForRequest serverSideEncryption) {
            this.serverSideEncryption = serverSideEncryption;
            return this;
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

        public Builder setB2Expires(LocalDateTime utcExpiration) {
            this.b2Expires = DateTimeFormatter.RFC_1123_DATE_TIME.format(utcExpiration.atOffset(ZoneOffset.UTC));
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
