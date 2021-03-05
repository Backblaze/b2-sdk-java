/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.exceptions.B2ForbiddenException;
import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class B2Bucket {
    @B2Json.required
    private final String accountId;

    @B2Json.required
    private final String bucketId;

    @B2Json.required
    private final String bucketName;

    @B2Json.optional
    private final String bucketType;

    @B2Json.optional
    private final Map<String,String> bucketInfo;

    @B2Json.optional
    private final List<B2CorsRule> corsRules;

    @B2Json.optional
    private final List<B2LifecycleRule> lifecycleRules;

    @B2Json.optional
    private final Set<String> options;

    @B2Json.optional
    private final B2BucketFileLockConfiguration defaultFileLockConfiguration;

    @B2Json.optional
    private final B2AuthorizationFilteredResponseField<B2BucketServerSideEncryption> defaultServerSideEncryption;

    @B2Json.required
    private final int revision;

    @B2Json.constructor(params = "accountId,bucketId,bucketName,bucketType,bucketInfo,corsRules,lifecycleRules," +
            "options,defaultFileLockConfiguration,defaultServerSideEncryption,revision")
    public B2Bucket(String accountId,
                    String bucketId,
                    String bucketName,
                    String bucketType,
                    Map<String, String> bucketInfo,
                    List<B2CorsRule> corsRules,
                    List<B2LifecycleRule> lifecycleRules,
                    Set<String> options,
                    B2BucketFileLockConfiguration defaultFileLockConfiguration,
                    B2AuthorizationFilteredResponseField<B2BucketServerSideEncryption> defaultServerSideEncryption,
                    int revision) {
        this.accountId = accountId;
        this.bucketId = bucketId;
        this.bucketName = bucketName;
        this.bucketType = bucketType;
        this.bucketInfo = bucketInfo;
        this.corsRules = corsRules;
        this.lifecycleRules = lifecycleRules;
        this.options = options;
        this.defaultFileLockConfiguration = defaultFileLockConfiguration;
        this.defaultServerSideEncryption = defaultServerSideEncryption;
        this.revision = revision;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getBucketType() {
        return bucketType;
    }

    public Map<String, String> getBucketInfo() {
        return bucketInfo;
    }

    public List<B2CorsRule> getCorsRules() {
        return corsRules;
    }

    public List<B2LifecycleRule> getLifecycleRules() {
        return lifecycleRules;
    }

    public Set<String> getOptions() {
        return options;
    }

    public int getRevision() {
        return revision;
    }

    public B2BucketFileLockConfiguration getDefaultFileLockConfiguration() {
        return defaultFileLockConfiguration;
    }

    /**
     * Indicates whether client is authorized to read default bucket encryption settings for bucket
     * @return true iff client is authorized to read value of defaultServerSideEncryption field in B2Bucket
     */
    public boolean isClientAuthorizedToReadDefaultServerSideEncryption() {
        if (defaultServerSideEncryption == null) {
            return false;
        }
        return defaultServerSideEncryption.isClientAuthorizedToRead();
    }

    /**
     * Returns settings for default bucket encryption (i.e., mode and algorithm) or null if there are none.
     * Throws B2ForbiddenException if client is not authorized to read bucket default encryption settings.
     * @return default bucket encryption settings
     * @throws B2ForbiddenException if client is not authorized to read defaultServerSideEncryption field
     */
    public B2BucketServerSideEncryption getDefaultServerSideEncryption() throws B2ForbiddenException {
        if (defaultServerSideEncryption == null) {
            return null;
        }
        // will throw B2ForbiddenException if client is not authorized to read value
        return defaultServerSideEncryption.getValue();
    }

    @Override
    public String toString() {
        return "B2Bucket(" +
                bucketName + "," +
                bucketType + "," +
                bucketId + "," +
                (bucketInfo == null ? 0 : bucketInfo.size()) + " infos," +
                (corsRules == null ? 0 : corsRules.size()) + " corsRules," +
                (lifecycleRules == null ? 0 : lifecycleRules.size()) + " lifecycleRules," +
                ((options == null || options.isEmpty()) ? 0 : "[" + String.join(", ", options) + "]") + " options," +
                (defaultFileLockConfiguration == null ? "null" : defaultFileLockConfiguration.toString()) + "," +
                (defaultServerSideEncryption == null ? "null" : defaultServerSideEncryption.toString()) + "," +
                "v" + revision +
                ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2Bucket b2Bucket = (B2Bucket) o;
        return getRevision() == b2Bucket.getRevision() &&
                Objects.equals(getAccountId(), b2Bucket.getAccountId()) &&
                Objects.equals(getBucketId(), b2Bucket.getBucketId()) &&
                Objects.equals(getBucketName(), b2Bucket.getBucketName()) &&
                Objects.equals(getBucketType(), b2Bucket.getBucketType()) &&
                Objects.equals(getBucketInfo(), b2Bucket.getBucketInfo()) &&
                Objects.equals(getCorsRules(), b2Bucket.getCorsRules()) &&
                Objects.equals(getLifecycleRules(), b2Bucket.getLifecycleRules()) &&
                Objects.equals(getOptions(), b2Bucket.getOptions()) &&
                Objects.equals(getDefaultFileLockConfiguration(), b2Bucket.getDefaultFileLockConfiguration()) &&
                Objects.equals(defaultServerSideEncryption, b2Bucket.defaultServerSideEncryption);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getAccountId(),
                getBucketId(),
                getBucketName(),
                getBucketType(),
                getBucketInfo(),
                getCorsRules(),
                getLifecycleRules(),
                getOptions(),
                getRevision(),
                getDefaultFileLockConfiguration(),
                defaultServerSideEncryption);
    }
}
