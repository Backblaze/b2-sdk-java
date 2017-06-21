/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2GetUploadPartUrlRequest {
    @B2Json.required
    private final String fileId;

    @B2Json.constructor(params = "fileId")
    public B2GetUploadPartUrlRequest(String fileId) {
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2GetUploadPartUrlRequest that = (B2GetUploadPartUrlRequest) o;
        return Objects.equals(getFileId(), that.getFileId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileId());
    }
}
