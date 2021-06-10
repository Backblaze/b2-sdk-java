/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2AccountAuthorization {
    @B2Json.required
    private final String accountId;
    @B2Json.required
    private final String authorizationToken;
    @B2Json.required
    private final String apiUrl;
    @B2Json.required
    private final String downloadUrl;
    @B2Json.required
    private final long recommendedPartSize;
    @B2Json.required
    private final long absoluteMinimumPartSize;
    @B2Json.required
    private final B2Allowed allowed;
    @B2Json.optional
    private final String s3ApiUrl;


    @B2Json.constructor(params = "accountId,authorizationToken,apiUrl,downloadUrl,recommendedPartSize,absoluteMinimumPartSize,allowed,s3ApiUrl")
    public B2AccountAuthorization(String accountId,
                                  String authorizationToken,
                                  String apiUrl,
                                  String downloadUrl,
                                  long recommendedPartSize,
                                  long absoluteMinimumPartSize,
                                  B2Allowed allowed,
                                  String s3ApiUrl) {
        this.accountId = accountId;
        this.authorizationToken = authorizationToken;
        this.apiUrl = apiUrl;
        this.downloadUrl = downloadUrl;
        this.recommendedPartSize = recommendedPartSize;
        this.absoluteMinimumPartSize = absoluteMinimumPartSize;
        this.allowed = allowed;
        this.s3ApiUrl = s3ApiUrl;
    }


    public String getAccountId() {
        return accountId;
    }

    public String getAuthorizationToken() {
        return authorizationToken;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public long getRecommendedPartSize() {
        return recommendedPartSize;
    }

    public long getAbsoluteMinimumPartSize() {
        return absoluteMinimumPartSize;
    }

    public B2Allowed getAllowed() {
        return allowed;
    }

    public String getS3ApiUrl() {
        return s3ApiUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2AccountAuthorization that = (B2AccountAuthorization) o;
        return getRecommendedPartSize() == that.getRecommendedPartSize() &&
                getAbsoluteMinimumPartSize() == that.getAbsoluteMinimumPartSize() &&
                Objects.equals(getAccountId(), that.getAccountId()) &&
                Objects.equals(getAuthorizationToken(), that.getAuthorizationToken()) &&
                Objects.equals(getApiUrl(), that.getApiUrl()) &&
                Objects.equals(getDownloadUrl(), that.getDownloadUrl()) &&
                Objects.equals(getAllowed(), that.getAllowed()) &&
                Objects.equals(getS3ApiUrl(), that.getS3ApiUrl());
    }

    @Override
    public String toString() {
        return "B2AccountAuthorization{" +
                "accountId='" + accountId + '\'' +
                ", authorizationToken='" + authorizationToken + '\'' +
                ", apiUrl='" + apiUrl + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", recommendedPartSize=" + recommendedPartSize +
                ", absoluteMinimumPartSize=" + absoluteMinimumPartSize +
                ", allowed=" + allowed +
                ", s3ApiUrl=" + s3ApiUrl +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getAccountId(),
                getAuthorizationToken(),
                getApiUrl(),
                getDownloadUrl(),
                getRecommendedPartSize(),
                getAbsoluteMinimumPartSize(),
                getAllowed(),
                getS3ApiUrl()
        );
    }

    /**
     * @param authToken the desired authToken.
     * @return a new B2AccountAuthorization, just like this one but with the
     *         given authToken.  useful for tests, etc.
     */
    public B2AccountAuthorization withAuthToken(String authToken) {
        return new B2AccountAuthorization(
                getAccountId(),
                authToken,
                getApiUrl(),
                getDownloadUrl(),
                getRecommendedPartSize(),
                getAbsoluteMinimumPartSize(),
                getAllowed(),
                getS3ApiUrl());
    }
}
