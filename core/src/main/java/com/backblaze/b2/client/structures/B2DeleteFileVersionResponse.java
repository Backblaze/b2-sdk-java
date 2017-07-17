/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2DeleteFileVersionResponse {
    @B2Json.required
    private final String fileId;

    @B2Json.required
    private final String fileName;

    @B2Json.constructor(params = "fileId,fileName")
    public B2DeleteFileVersionResponse(String fileId,
                                       String fileName) {
        this.fileId = fileId;
        this.fileName = fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2DeleteFileVersionResponse that = (B2DeleteFileVersionResponse) o;
        return Objects.equals(getFileId(), that.getFileId()) &&
                Objects.equals(getFileName(), that.getFileName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileId(), getFileName());
    }

    @Override
    public String toString() {
        return "B2DeleteFileVersionResponse{" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}
