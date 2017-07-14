/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2UploadUrlResponse {
    @B2Json.required
    private final String bucketId;
    @B2Json.required
    private final String uploadUrl;
    @B2Json.required
    private final String authorizationToken;

    @B2Json.constructor(params = "bucketId,uploadUrl,authorizationToken")
    public B2UploadUrlResponse(String bucketId,
                               String uploadUrl,
                               String authorizationToken) {
        this.bucketId = bucketId;
        this.uploadUrl = uploadUrl;
        this.authorizationToken = authorizationToken;
    }

    public String getBucketId() {
        return bucketId;
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
        B2UploadUrlResponse response = (B2UploadUrlResponse) o;
        return Objects.equals(getBucketId(), response.getBucketId()) &&
                Objects.equals(getUploadUrl(), response.getUploadUrl()) &&
                Objects.equals(getAuthorizationToken(), response.getAuthorizationToken());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBucketId(), getUploadUrl(), getAuthorizationToken());
    }
}