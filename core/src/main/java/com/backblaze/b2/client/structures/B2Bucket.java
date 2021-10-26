/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.exceptions.B2ForbiddenException;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

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

    @B2Json.required
    private final B2AuthorizationFilteredResponseField<B2BucketFileLockConfiguration> fileLockConfiguration;

    @B2Json.required
    private final B2AuthorizationFilteredResponseField<B2BucketServerSideEncryption> defaultServerSideEncryption;

    @B2Json.optional
    private final B2AuthorizationFilteredResponseField<B2BucketReplicationConfiguration> replicationConfiguration;

    @B2Json.required
    private final int revision;

    @B2Json.constructor(params = "accountId,bucketId,bucketName,bucketType,bucketInfo,corsRules,lifecycleRules," +
            "options,fileLockConfiguration,defaultServerSideEncryption,replicationConfiguration,revision")
    public B2Bucket(String accountId,
                    String bucketId,
                    String bucketName,
                    String bucketType,
                    Map<String, String> bucketInfo,
                    List<B2CorsRule> corsRules,
                    List<B2LifecycleRule> lifecycleRules,
                    Set<String> options,
                    B2AuthorizationFilteredResponseField<B2BucketFileLockConfiguration> fileLockConfiguration,
                    B2AuthorizationFilteredResponseField<B2BucketServerSideEncryption> defaultServerSideEncryption,
                    B2AuthorizationFilteredResponseField<B2BucketReplicationConfiguration> replicationConfiguration,
                    int revision) {
        this.accountId = accountId;
        this.bucketId = bucketId;
        this.bucketName = bucketName;
        this.bucketType = bucketType;
        this.bucketInfo = bucketInfo;
        this.corsRules = corsRules;
        this.lifecycleRules = lifecycleRules;
        this.options = options;
        this.fileLockConfiguration = fileLockConfiguration;
        this.defaultServerSideEncryption = defaultServerSideEncryption;
        this.replicationConfiguration = replicationConfiguration;
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

    /**
     * Indicates whether client is authorized to read file lock configuration settings for bucket
     * @return true iff client is authorized to read value of fileLockConfiguration field in B2Bucket
     */
    public boolean isClientAuthorizedToReadFileLockConfiguration() {
        B2Preconditions.checkState(fileLockConfiguration != null);

        return fileLockConfiguration.isClientAuthorizedToRead();
    }

    /**
     * Returns bucket file lock configuration. Throws B2ForbiddenException if client is not authorized to read
     * bucket file lock configuration.
     * @return file lock configuration
     * @throws B2ForbiddenException if client is not authorized to read fileLockConfiguration
     */
    public B2BucketFileLockConfiguration getFileLockConfiguration() throws B2ForbiddenException {
        B2Preconditions.checkState(fileLockConfiguration != null);

        // will throw B2ForbiddenException if client is not authorized to read value
        return fileLockConfiguration.getValue();
    }

    /**
     * Indicates whether client is authorized to read default bucket encryption settings for bucket
     * @return true iff client is authorized to read value of defaultServerSideEncryption field in B2Bucket
     */
    public boolean isClientAuthorizedToReadDefaultServerSideEncryption() {
        B2Preconditions.checkState(defaultServerSideEncryption != null);

        return defaultServerSideEncryption.isClientAuthorizedToRead();
    }

    /**
     * Returns settings for default bucket encryption (i.e., mode and algorithm) or null if there are none.
     * Throws B2ForbiddenException if client is not authorized to read bucket default encryption settings.
     * @return default bucket encryption settings
     * @throws B2ForbiddenException if client is not authorized to read defaultServerSideEncryption field
     */
    public B2BucketServerSideEncryption getDefaultServerSideEncryption() throws B2ForbiddenException {
        B2Preconditions.checkState(defaultServerSideEncryption != null);

        // will throw B2ForbiddenException if client is not authorized to read value
        return defaultServerSideEncryption.getValue();
    }

    /**
     * Indicates whether client is authorized to read replication configuration settings for bucket
     * @return true iff client is authorized to read value of replicationConfiguration field in B2Bucket
     */
    public boolean isClientAuthorizedToReadReplicationConfiguration() {
        B2Preconditions.checkState(replicationConfiguration != null);

        return replicationConfiguration.isClientAuthorizedToRead();
    }

    /**
     * Returns settings for bucket replication configuration or null if there are none.
     * Throws B2ForbiddenException if client is not authorized to read bucket replication configuration settings.
     * @return replication configuration settings
     * @throws B2ForbiddenException if client is not authorized to read replicationConfiguration field
     */
    public B2BucketReplicationConfiguration getReplicationConfiguration() throws B2ForbiddenException {
        B2Preconditions.checkState(replicationConfiguration != null);

        // will throw B2ForbiddenException if client is not authorized to read value
        return replicationConfiguration.getValue();
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
                fileLockConfiguration + "," +
                defaultServerSideEncryption + "," +
                replicationConfiguration + "," +
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
                // don't use getter for these two fields because they can throw B2ForbiddenException
                Objects.equals(fileLockConfiguration, b2Bucket.fileLockConfiguration) &&
                Objects.equals(defaultServerSideEncryption, b2Bucket.defaultServerSideEncryption) &&
                Objects.equals(replicationConfiguration, b2Bucket.replicationConfiguration);
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
                // don't use getter for these two fields because they can throw B2ForbiddenException
                fileLockConfiguration,
                defaultServerSideEncryption,
                replicationConfiguration
        );
    }
}
