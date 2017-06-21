/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Objects;

public class B2ListFileVersionsResponse implements B2ListFilesResponse {
    @B2Json.required
    private final List<B2FileVersion> files;

    @B2Json.optional
    private final String nextFileName;

    @B2Json.optional
    private final String nextFileId;


    @B2Json.constructor(params = "files,nextFileName,nextFileId")
    public B2ListFileVersionsResponse(List<B2FileVersion> files,
                                      String nextFileName,
                                      String nextFileId) {
        this.files = files;
        this.nextFileName = nextFileName;
        this.nextFileId = nextFileId;
    }

    @Override
    public List<B2FileVersion> getFiles() {
        return files;
    }

    @Override
    public boolean atEnd() {
        return (nextFileName == null);
    }

    public String getNextFileName() {
        return nextFileName;
    }

    public String getNextFileId() {
        return nextFileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2ListFileVersionsResponse that = (B2ListFileVersionsResponse) o;
        return Objects.equals(getFiles(), that.getFiles()) &&
                Objects.equals(getNextFileName(), that.getNextFileName()) &&
                Objects.equals(getNextFileId(), that.getNextFileId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFiles(), getNextFileName(), getNextFileId());
    }
}
