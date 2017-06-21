/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Objects;

public class B2FinishLargeFileRequest {
    @B2Json.required
    private final String fileId;
    @B2Json.required
    private final List<String> partSha1Array;

    @B2Json.constructor(params = "fileId,partSha1Array")
    public B2FinishLargeFileRequest(String fileId,
                                    List<String> partSha1Array) {
        this.fileId = fileId;
        this.partSha1Array = partSha1Array;
    }

    public String getFileId() {
        return fileId;
    }

    public List<String> getPartSha1Array() {
        return partSha1Array;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2FinishLargeFileRequest that = (B2FinishLargeFileRequest) o;
        return Objects.equals(getFileId(), that.getFileId()) &&
                Objects.equals(getPartSha1Array(), that.getPartSha1Array());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileId(), getPartSha1Array());
    }

    public static Builder builder(String fileId,
                                  List<String> partSha1Array) {
        return new Builder(fileId, partSha1Array);
    }

    public static class Builder {
        private final String fileId;
        private final List<String> partSha1Array;

        public Builder(String fileId,
                       List<String> partSha1Array) {
            this.fileId = fileId;
            this.partSha1Array = partSha1Array;
        }

        public B2FinishLargeFileRequest build() {
            return new B2FinishLargeFileRequest(fileId, partSha1Array);
        }
    }
}
