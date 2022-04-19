/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2ForbiddenException;
import com.backblaze.b2.json.B2Json;

import java.util.Map;
import java.util.Objects;

/**
 * Information about a file, as returned by the B2 API.
 *
 * The API returns two fields that are not included here: accountId and bucketId.
 * The reason for not including them is that this SDK also returns the same
 * structure from getFileInfoByName, which gets the info from the headers returned
 * by a HEAD request on the file, which do not include them.
 */
public class B2FileVersion {

    public static final String UPLOAD_ACTION = "upload";
    public static final String HIDE_ACTION = "hide";
    public static final String START_ACTION = "start";
    public static final String FOLDER_ACTION = "folder";

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
    @B2Json.optional // for example, "folder"s don't have contentMd5s nor do largeFiles.
    private final String contentMd5;
    @B2Json.optional
    private final Map<String,String> fileInfo;
    @B2Json.optional  // for example, large files don't have action in response from b2_start_large_file.
    private final String action;
    @B2Json.required
    private final long uploadTimestamp;
    @B2Json.optional
    private final B2AuthorizationFilteredResponseField<B2FileRetention> fileRetention;
    @B2Json.optional
    private final B2AuthorizationFilteredResponseField<String> legalHold;
    @B2Json.optional
    private final B2FileSseForResponse serverSideEncryption;
    @B2Json.optional
    private final String replicationStatus;

    @B2Json.constructor(params = "fileId,fileName,contentLength,contentType," +
            "contentSha1,contentMd5,fileInfo,action,uploadTimestamp,fileRetention," +
            "legalHold,serverSideEncryption,replicationStatus")
    public B2FileVersion(String fileId,
                         String fileName,
                         long contentLength,
                         String contentType,
                         String contentSha1,
                         String contentMd5,
                         Map<String, String> fileInfo,
                         String action,
                         long uploadTimestamp,
                         B2AuthorizationFilteredResponseField<B2FileRetention> fileRetention,
                         B2AuthorizationFilteredResponseField<String> legalHold,
                         B2FileSseForResponse serverSideEncryption,
                         String replicationStatus) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.contentSha1 = contentSha1;
        this.contentMd5 = contentMd5;
        this.fileInfo = fileInfo;
        this.action = action;
        this.uploadTimestamp = uploadTimestamp;
        this.fileRetention = fileRetention;
        this.legalHold = legalHold;
        this.serverSideEncryption = serverSideEncryption;
        this.replicationStatus = replicationStatus;
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

    public String getContentMd5() {
        return contentMd5;
    }

    public String getLargeFileSha1OrNull() {
        return fileInfo.get(B2Headers.LARGE_FILE_SHA1_INFO_NAME);
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

    /**
     * Indicates whether or not the client is authorized to read the file retention
     * from this file version. If fileRetention field is null (e.g., for hidden
     * files and folders), then this method returns true.
     *
     * @return true iff the client is authorized to read value of the file retention
     */
    public boolean isClientAuthorizedToReadFileRetention() {
        if (fileRetention == null) {
            return true;
        }
        return fileRetention.isClientAuthorizedToRead();
    }

    /**
     * Returns the file retention setting on the file version
     *
     * @return the file retention settings of the file version
     * @throws B2ForbiddenException if the client is not authorized to read the file retention setting
     */
    public B2FileRetention getFileRetention() throws B2ForbiddenException {
        return fileRetention == null ? null : fileRetention.getValue();
    }

    /**
     * Indicates whether or not the client is authorized to read the legal hold
     * status from this file version. If legalHold field is null (e.g., for hidden
     * files and folders), then this method returns true.
     *
     * @return true iff the client is authorized to read value of the legal hold status
     */
    public boolean isClientAuthorizedToReadLegalHold() {
        if (legalHold == null) {
            return true;
        }
        return legalHold.isClientAuthorizedToRead();
    }

    /**
     * Returns the legal hold status on the file version
     *
     * @return the legal hold status of the file version
     * @throws B2ForbiddenException if the client is not authorized to read the legal hold status
     */
    public String getLegalHold() throws B2ForbiddenException {
        return legalHold == null ? null : legalHold.getValue();
    }

    public B2FileSseForResponse getServerSideEncryption() { return serverSideEncryption; }

    public String getReplicationStatus() {
        return replicationStatus;
    }

    public boolean isUpload() {
        return UPLOAD_ACTION.equals(action);
    }

    public boolean isHide() {
        return HIDE_ACTION.equals(action);
    }

    public boolean isStart() {
        return START_ACTION.equals(action);
    }

    public boolean isFolder() {
        return FOLDER_ACTION.equals(action);
    }

    @Override
    public String toString() {
        return "B2FileVersion{" +
                "fileId='" + fileId + "', " +
                "contentLength=" + contentLength + ", " +
                "contentType='" + contentType + "', " +
                "contentSha1='" + contentSha1 + "', " +
                "contentMd5='" + contentMd5 + "', " +
                "action='" + action + "', " +
                "uploadTimestamp=" + uploadTimestamp + ", " +
                "fileInfo=[" + (fileInfo != null ? fileInfo.size() : "") + "], " +
                "fileName='" + fileName + "', " +
                "fileRetention='" + fileRetention + "', " +
                "legalHold='" + legalHold + "', " +
                "serverSideEncryption='" + serverSideEncryption + "', " +
                "replicationStatus='" + replicationStatus + "'" +
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
                Objects.equals(getContentMd5(), that.getContentMd5()) &&
                Objects.equals(getFileInfo(), that.getFileInfo()) &&
                Objects.equals(getAction(), that.getAction()) &&
                Objects.equals(fileRetention, that.fileRetention) && // compare the complete AuthorizationFilteredResponseField
                Objects.equals(legalHold, that.legalHold) && // compare the complete AuthorizationFilteredResponseField
                Objects.equals(getServerSideEncryption(), that.getServerSideEncryption()) &&
                Objects.equals(getReplicationStatus(), that.getReplicationStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getFileId(),
                getFileName(),
                getContentLength(),
                getContentType(),
                getContentSha1(),
                getContentMd5(),
                getFileInfo(),
                getAction(),
                getUploadTimestamp(),
                fileRetention,
                legalHold,
                getServerSideEncryption(),
                getReplicationStatus()
        );
    }
}
