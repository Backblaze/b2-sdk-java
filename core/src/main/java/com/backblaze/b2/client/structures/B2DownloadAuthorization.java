/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

public class B2DownloadAuthorization {
    @B2Json.required
    private final String bucketId;
    @B2Json.required
    private final String fileNamePrefix;
    @B2Json.required
    private final String authorizationToken;

    @B2Json.constructor(params = "bucketId,fileNamePrefix,authorizationToken")
    public B2DownloadAuthorization(String bucketId,
                                   String fileNamePrefix,
                                   String authorizationToken) {
        this.bucketId = bucketId;
        this.fileNamePrefix = fileNamePrefix;
        this.authorizationToken = authorizationToken;
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }
}
