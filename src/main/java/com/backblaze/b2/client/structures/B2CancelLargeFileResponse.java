/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2CancelLargeFileResponse {
    @B2Json.required
    private final String fileId;
    @B2Json.required
    private final String bucketId;
    @B2Json.required
    private final String fileName;

    @B2Json.constructor(params = "fileId,bucketId,fileName")
    public B2CancelLargeFileResponse(String fileId,
                                     String bucketId,
                                     String fileName) {
        this.fileId = fileId;
        this.bucketId = bucketId;
        this.fileName = fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return "B2FileVersion{" +
                "fileId='" + fileId + '\'' +
                ", bucketId='" + bucketId + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2CancelLargeFileResponse response = (B2CancelLargeFileResponse) o;
        return Objects.equals(getFileId(), response.getFileId()) &&
                Objects.equals(bucketId, response.bucketId) &&
                Objects.equals(getFileName(), response.getFileName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileId(), bucketId, getFileName());
    }
}
