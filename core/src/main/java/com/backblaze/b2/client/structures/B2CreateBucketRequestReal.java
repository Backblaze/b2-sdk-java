/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class B2CreateBucketRequestReal {
    @B2Json.required
    private final String accountId;

    @B2Json.required
    private final String bucketName;

    @B2Json.optional
    private final String bucketType;

    @B2Json.optional
    private final Map<String, String> bucketInfo;

    @B2Json.optional
    private final List<B2LifecycleRule> lifecycleRules;


    @B2Json.constructor(params = "accountId,bucketName,bucketType,bucketInfo,lifecycleRules")
    private B2CreateBucketRequestReal(String accountId,
                                      String bucketName,
                                      String bucketType,
                                      Map<String, String> bucketInfo,
                                      List<B2LifecycleRule> lifecycleRules) {
        this.accountId = accountId;
        this.bucketName = bucketName;
        this.bucketType = bucketType;
        this.bucketInfo = bucketInfo;
        this.lifecycleRules = lifecycleRules;
    }

    public B2CreateBucketRequestReal(String accountId,
                                     B2CreateBucketRequest mostOfRequest) {
        this(accountId,
                mostOfRequest.getBucketName(),
                mostOfRequest.getBucketType(),
                mostOfRequest.getBucketInfo(),
                mostOfRequest.getLifecycleRules());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2CreateBucketRequestReal that = (B2CreateBucketRequestReal) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(bucketName, that.bucketName) &&
                Objects.equals(bucketType, that.bucketType) &&
                Objects.equals(bucketInfo, that.bucketInfo) &&
                Objects.equals(lifecycleRules, that.lifecycleRules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, bucketName, bucketType, bucketInfo, lifecycleRules);
    }
}
