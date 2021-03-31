/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2UpdateFileRetentionResponse {
    @B2Json.required
    public final String fileName;

    @B2Json.required
    public final String fileId;

    @B2Json.required
    public final B2FileRetention fileRetention;

    @B2Json.constructor(params = "fileName, fileId, fileRetention")
    public B2UpdateFileRetentionResponse(String fileName,
                                         String fileId,
                                         B2FileRetention fileRetention) {
        this.fileName = fileName;
        this.fileId = fileId;
        this.fileRetention = fileRetention;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public B2FileRetention getFileRetention() {
        return fileRetention;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2UpdateFileRetentionResponse that = (B2UpdateFileRetentionResponse) o;
        return Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getFileId(), that.getFileId()) &&
                Objects.equals(getFileRetention(), that.getFileRetention());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileName(), getFileId(), getFileRetention());
    }

    @Override
    public String toString() {
        return "B2UpdateFileLegalHoldResponse {" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileRetention='" + fileRetention + '\'' +
                '}';
    }
}
