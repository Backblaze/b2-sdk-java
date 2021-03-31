/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

public class B2UpdateFileLegalHoldRequest {
    @B2Json.required
    public final String fileName;

    @B2Json.required
    public final String fileId;

    @B2Json.required
    public final String legalHold;

    @B2Json.constructor(params = "fileName, fileId, legalHold")
    private B2UpdateFileLegalHoldRequest(String fileName,
                                         String fileId,
                                         String legalHold) {
        B2Preconditions.checkArgument(
                B2LegalHold.ON.equals(legalHold) || B2LegalHold.OFF.equals(legalHold),
                String.format("Invalid legalHold value. Valid values: %s, %s", B2LegalHold.ON, B2LegalHold.OFF)
        );

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
        B2UpdateFileLegalHoldRequest that = (B2UpdateFileLegalHoldRequest) o;
        return Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getFileId(), that.getFileId()) &&
                Objects.equals(getLegalHold(), that.getLegalHold());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileName(), getFileId(), getLegalHold());
    }

    public static Builder builder(String fileName,
                                  String fileId,
                                  String legalHold) {
        return new Builder(fileName, fileId, legalHold);
    }

    public static class Builder {
        private final String fileName;
        private final String fileId;
        private final String legalHold;

        public Builder(String fileName,
                       String fileId,
                       String legalHold) {
            this.fileName = fileName;
            this.fileId = fileId;
            this.legalHold = legalHold;
        }

        public B2UpdateFileLegalHoldRequest build() {
            return new B2UpdateFileLegalHoldRequest(
                    fileName,
                    fileId,
                    legalHold);
        }
    }
}
