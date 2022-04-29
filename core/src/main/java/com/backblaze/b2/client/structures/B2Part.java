/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2Part {
    @B2Json.required
    private final String fileId;
    @B2Json.required
    private final int partNumber;
    @B2Json.required
    private final long contentLength;
    @B2Json.required
    private final String contentSha1;
    @B2Json.optional
    private final String contentMd5;
    @B2Json.optional  // not present in response from b2_upload_part.
    private final long uploadTimestamp;
    @B2Json.optional
    private final B2FileSseForResponse serverSideEncryption;

    @B2Json.constructor(params = "fileId,partNumber,contentLength,contentSha1,contentMd5,uploadTimestamp,serverSideEncryption")
    public B2Part(String fileId,
                  int partNumber,
                  long contentLength,
                  String contentSha1,
                  String contentMd5,
                  long uploadTimestamp,
                  B2FileSseForResponse serverSideEncryption) {
        this.fileId = fileId;
        this.partNumber = partNumber;
        this.contentLength = contentLength;
        this.contentSha1 = contentSha1;
        this.contentMd5 = contentMd5;
        this.uploadTimestamp = uploadTimestamp;
        this.serverSideEncryption = serverSideEncryption;
    }


    public String getFileId() {
        return fileId;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentSha1() {
        return contentSha1;
    }

    public String getContentMd5() {
        return contentMd5;
    }

    public long getUploadTimestamp() {
        return uploadTimestamp;
    }

    public B2FileSseForResponse getServerSideEncryption() {
        return serverSideEncryption;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2Part b2Part = (B2Part) o;
        return getPartNumber() == b2Part.getPartNumber() &&
                getContentLength() == b2Part.getContentLength() &&
                getUploadTimestamp() == b2Part.getUploadTimestamp() &&
                Objects.equals(getFileId(), b2Part.getFileId()) &&
                Objects.equals(getContentSha1(), b2Part.getContentSha1()) &&
                Objects.equals(getContentMd5(), b2Part.getContentMd5()) &&
                Objects.equals(getServerSideEncryption(), b2Part.getServerSideEncryption());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileId(), getPartNumber(), getContentLength(), getContentSha1(), getContentMd5(), getUploadTimestamp(), getServerSideEncryption());
    }

    @Override
    public String toString() {
        return "B2Part{" +
                "fileId='" + fileId + '\'' +
                ", partNumber='" + partNumber + '\'' +
                ", contentLength=" + contentLength +
                ", contentSha1='" + contentSha1 + '\'' +
                ", contentMd5='" + contentMd5 + '\'' +
                ", uploadTimestamp=" + uploadTimestamp +
                ", serverSideEncryption=" + serverSideEncryption +
                '}';
    }
}
