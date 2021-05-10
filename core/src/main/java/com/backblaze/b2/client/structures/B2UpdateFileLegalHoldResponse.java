/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2UpdateFileLegalHoldResponse {
    @B2Json.required
    public final String fileName;

    @B2Json.required
    public final String fileId;

    @B2Json.required
    public final String legalHold;

    @B2Json.constructor(params = "fileName, fileId, legalHold")
    public B2UpdateFileLegalHoldResponse(String fileName,
                                         String fileId,
                                         String legalHold) {
        this.fileName = fileName;
        this.fileId = fileId;
        this.legalHold = legalHold;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public String getLegalHold() {
        return legalHold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2UpdateFileLegalHoldResponse that = (B2UpdateFileLegalHoldResponse) o;
        return Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getFileId(), that.getFileId()) &&
                Objects.equals(getLegalHold(), that.getLegalHold());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileName(), getFileId(), getLegalHold());
    }

    @Override
    public String toString() {
        return "B2UpdateFileLegalHoldResponse {" +
                "fileId='" + fileId + '\'' +
                ", fileName='" + fileName + '\'' +
                ", legalHold='" + legalHold + '\'' +
                '}';
    }
}
