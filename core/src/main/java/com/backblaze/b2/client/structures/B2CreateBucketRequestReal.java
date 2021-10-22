/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * NOTE:
 * B2CreateBucketRequestReal has the attributes needed by the B2 API.  That's why it's name ends with 'Real'.
 * Code that calls B2StorageClient uses B2CreateBucketRequest (with no 'Real' at the end) instead.
 * The B2StorageClient creates a 'Real' request by adding the accountId to the non-real version before
 * sending it to the webifier.
 */

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
    private final List<B2CorsRule> corsRules;

    @B2Json.optional
    private final List<B2LifecycleRule> lifecycleRules;

    @B2Json.optional
    private final boolean fileLockEnabled;

    @B2Json.optional
    private final B2BucketServerSideEncryption defaultServerSideEncryption;

    @B2Json.optional
    private final B2BucketReplicationConfiguration replicationConfiguration;

    @B2Json.constructor(params = "accountId,bucketName,bucketType,bucketInfo,corsRules,lifecycleRules,fileLockEnabled,"+
            "defaultServerSideEncryption, replicationConfiguration")
    private B2CreateBucketRequestReal(String accountId,
                                      String bucketName,
                                      String bucketType,
                                      Map<String, String> bucketInfo,
                                      List<B2CorsRule> corsRules,
                                      List<B2LifecycleRule> lifecycleRules,
                                      boolean fileLockEnabled,
                                      B2BucketServerSideEncryption defaultServerSideEncryption,
                                      B2BucketReplicationConfiguration replicationConfiguration) {
        this.accountId = accountId;
        this.bucketName = bucketName;
        this.bucketType = bucketType;
        this.bucketInfo = bucketInfo;
        this.corsRules = corsRules;
        this.lifecycleRules = lifecycleRules;
        this.fileLockEnabled = fileLockEnabled;
        this.defaultServerSideEncryption = defaultServerSideEncryption;
        this.replicationConfiguration = replicationConfiguration;
    }

    public B2CreateBucketRequestReal(String accountId,
                                     B2CreateBucketRequest mostOfRequest) {
        this(accountId,
                mostOfRequest.getBucketName(),
                mostOfRequest.getBucketType(),
                mostOfRequest.getBucketInfo(),
                mostOfRequest.getCorsRules(),
                mostOfRequest.getLifecycleRules(),
                mostOfRequest.isFileLockEnabled(),
                mostOfRequest.getDefaultServerSideEncryption(),
                mostOfRequest.getReplicationConfiguration()
        );
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
                Objects.equals(corsRules, that.corsRules) &&
                Objects.equals(lifecycleRules, that.lifecycleRules) &&
                Objects.equals(fileLockEnabled, that.fileLockEnabled) &&
                Objects.equals(defaultServerSideEncryption, that.defaultServerSideEncryption) &&
                Objects.equals(replicationConfiguration, that.replicationConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                accountId,
                bucketName,
                bucketType,
                bucketInfo,
                corsRules,
                lifecycleRules,
                fileLockEnabled,
                defaultServerSideEncryption,
                replicationConfiguration
        );
    }

    @Override
    public String toString() {
        return "B2CreateBucketRequestReal{" +
                "accountId='" + accountId + '\'' +
                ", bucketName='" + bucketName + '\'' +
                ", bucketType='" + bucketType + '\'' +
                ", bucketInfo=" + bucketInfo +
                ", corsRules=" + corsRules +
                ", lifecycleRules=" + lifecycleRules +
                ", fileLockEnabled=" + fileLockEnabled +
                ", defaultServerSideEncryption=" + defaultServerSideEncryption +
                ", replicationConfiguration=" + replicationConfiguration +
                '}';
    }
}
