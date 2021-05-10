/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

public class B2UpdateFileRetentionRequest {
    @B2Json.required
    public final String fileName;

    @B2Json.required
    public final String fileId;

    @B2Json.optional
    public final boolean bypassGovernance;

    @B2Json.required
    public final B2FileRetention fileRetention;

    @B2Json.constructor(params = "fileName, fileId, bypassGovernance, fileRetention")
    private B2UpdateFileRetentionRequest(String fileName,
                                         String fileId,
                                         boolean bypassGovernance,
                                         B2FileRetention fileRetention) {
        // perform some simple validation checks:
        // 1) make sure mode is valid (i.e., if non-null, then either governance or compliance)
        B2Preconditions.checkArgument(fileRetention.getMode() == null ||
                B2FileRetentionMode.COMPLIANCE.equals(fileRetention.getMode()) ||
                B2FileRetentionMode.GOVERNANCE.equals(fileRetention.getMode()),
                "Invalid value for file retention mode");
        // 2) Both mode and retainUntilTimestamp must be either null or non-null
        B2Preconditions.checkArgument(
                (fileRetention.getMode() != null && fileRetention.getRetainUntilTimestamp() != null) ||
                        (fileRetention.getMode() == null && fileRetention.getRetainUntilTimestamp() == null),
                "Both file retention mode and retainUntilTimestamp are required if either is supplied");

        this.fileName = fileName;
        this.fileId = fileId;
        this.bypassGovernance = bypassGovernance;
        this.fileRetention = fileRetention;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileId() {
        return fileId;
    }

    public boolean isBypassGovernance() {
        return bypassGovernance;
    }

    public B2FileRetention getFileRetention() {
        return fileRetention;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2UpdateFileRetentionRequest that = (B2UpdateFileRetentionRequest) o;
        return Objects.equals(getFileName(), that.getFileName()) &&
                Objects.equals(getFileId(), that.getFileId()) &&
                isBypassGovernance() == that.isBypassGovernance() &&
                Objects.equals(getFileRetention(), that.getFileRetention());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileName(), getFileId(), isBypassGovernance(), getFileRetention());
    }

    public static Builder builder(String fileName,
                                  String fileId,
                                  B2FileRetention fileRetention) {
        return new Builder(fileName, fileId, fileRetention);
    }

    public static class Builder {
        private final String fileName;
        private final String fileId;
        private final B2FileRetention fileRetention;

        private boolean bypassGovernance;

        public Builder(String fileName,
                       String fileId,
                       B2FileRetention fileRetention) {
            this.fileName = fileName;
            this.fileId = fileId;
            this.fileRetention = fileRetention;
        }

        public Builder setBypassGovernance(boolean bypassGovernance) {
            this.bypassGovernance = bypassGovernance;
            return this;
        }

        public B2UpdateFileRetentionRequest build() {
            return new B2UpdateFileRetentionRequest(
                    fileName,
                    fileId,
                    bypassGovernance,
                    fileRetention);
        }
    }
}
