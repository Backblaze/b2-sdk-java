/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Map;
import java.util.Objects;

public class B2FileVersion {
    @B2Json.optional // for example, "folder"s don't have fileIds
    private final String fileId;
    @B2Json.required
    private final String fileName;
    @B2Json.optional  // for example, large files don't have action in response from b2_start_large_file.
    private final long contentLength;
    @B2Json.optional  // for example, hidden files, "folder"s don't have contentType
    private final String contentType;
    @B2Json.optional // for example, "folder"s don't have contentSha1s nor do largeFiles.
    private final String contentSha1;
    @B2Json.optional
    private final Map<String,String> fileInfo;
    @B2Json.optional  // for example, large files don't have action in response from b2_start_large_file.
    private final String action;
    @B2Json.required
    private final long uploadTimestamp;

    @B2Json.constructor(params = "fileId,fileName,contentLength,contentType," +
            "contentSha1,fileInfo,action,uploadTimestamp")
    public B2FileVersion(String fileId,
                         String fileName,
                         long contentLength,
                         String contentType,
                         String contentSha1,
                         Map<String, String> fileInfo,
                         String action,
                         long uploadTimestamp) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.contentSha1 = contentSha1;
        this.fileInfo = fileInfo;
        this.action = action;
        this.uploadTimestamp = uploadTimestamp;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentSha1() {
        return contentSha1;
    }

    public Map<String, String> getFileInfo() {
        return fileInfo;
    }

    public String getAction() {
        return action;
    }

    public long getUploadTimestamp() {
        return uploadTimestamp;
    }

    @Override
    public String toString() {
        return "B2FileVersion{" +
                "fileId='" + fileId + '\'' +
                ", contentLength=" + contentLength +
                ", contentType='" + contentType + '\'' +
                ", contentSha1='" + contentSha1 + '\'' +
                ", action='" + action + '\'' +
                ", uploadTimestamp=" + uploadTimestamp +
                ", fileInfo=[" + fileInfo.size() + "]" +
                ", fileName='" + fileName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2FileVersion that = (B2FileVersion) o;
        return getContentLength() == that.getContentLength() &&
                getUploadTimestamp() == that.getUploadTimestamp() &&
                Objects.equals(getFileId(), that.getFileId()) &&
                Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getContentType(), that.getContentType()) &&
                Objects.equals(getContentSha1(), that.getContentSha1()) &&
                Objects.equals(getFileInfo(), that.getFileInfo()) &&
                Objects.equals(getAction(), that.getAction());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileId(), getFileName(), getContentLength(), getContentType(), getContentSha1(), getFileInfo(), getAction(), getUploadTimestamp());
    }
}
