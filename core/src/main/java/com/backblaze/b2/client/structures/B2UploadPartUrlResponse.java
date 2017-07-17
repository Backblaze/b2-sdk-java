/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2UploadPartUrlResponse {
    @B2Json.required
    private final String fileId;
    @B2Json.required
    private final String uploadUrl;
    @B2Json.required
    private final String authorizationToken;

    @B2Json.constructor(params = "fileId,uploadUrl,authorizationToken")
    public B2UploadPartUrlResponse(String fileId,
                                   String uploadUrl,
                                   String authorizationToken) {
        this.fileId = fileId;
        this.uploadUrl = uploadUrl;
        this.authorizationToken = authorizationToken;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2UploadPartUrlResponse that = (B2UploadPartUrlResponse) o;
        return Objects.equals(fileId, that.fileId) &&
                Objects.equals(getUploadUrl(), that.getUploadUrl()) &&
                Objects.equals(getAuthorizationToken(), that.getAuthorizationToken());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, getUploadUrl(), getAuthorizationToken());
    }

    public String getFileId() {
        return fileId;
    }
}