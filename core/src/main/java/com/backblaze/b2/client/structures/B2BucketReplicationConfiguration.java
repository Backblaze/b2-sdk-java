/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Replication configuration for a bucket.
 *
 * Factory methods are available to create configurations for 1) source-only,
 * 2) destination-only, or 3) simultaneously source and destination buckets.
 */
public class B2BucketReplicationConfiguration {
    @B2Json.optional
    private final B2SourceConfig asReplicationSource;

    @B2Json.optional
    private final B2DestinationConfig asReplicationDestination;

    /**
     * Static field for convenience to use with updateBucket() to remove bucket replication configuration
     */
    public static final B2BucketReplicationConfiguration NONE =
            new B2BucketReplicationConfiguration(null, null);

    @B2Json.constructor(params = "asReplicationSource, asReplicationDestination")
    private B2BucketReplicationConfiguration(B2SourceConfig asReplicationSource,
                                             B2DestinationConfig asReplicationDestination) {
        this.asReplicationSource = asReplicationSource;
        this.asReplicationDestination = asReplicationDestination;
    }

    /**
     * Returns a ReplicationConfiguration for a bucket that will be a replication source <b>and</b> a replication
     * destination. All arguments must be non-null, and replicationRules and sourceToDestinationKeyMapping must
     * not be empty.
     * @throws IllegalArgumentException if any input arguments are null
     */
    public static B2BucketReplicationConfiguration createForSourceAndDestination(String sourceApplicationKeyId,
                                                                                 List<B2ReplicationRule> replicationRules,
                                                                                 Map<String, String> sourceToDestinationKeyMapping) {
        return new B2BucketReplicationConfiguration(
                new B2SourceConfig(sourceApplicationKeyId, replicationRules),
                new B2DestinationConfig(sourceToDestinationKeyMapping)
        );
    }

    /**
     * Returns a B2BucketReplicationConfiguration for a bucket that will only be a replication source. Both arguments
     * must be non-null and replicationRules must not be empty.
     * @throws IllegalArgumentException if any input arguments are null or replicationRules is empty
     */
    public static B2BucketReplicationConfiguration createForSource(String sourceApplicationKeyId,
                                                                   List<B2ReplicationRule> replicationRules) {
        return new B2BucketReplicationConfiguration(
                new B2SourceConfig(sourceApplicationKeyId, replicationRules),
                null
        );
    }

    /**
     * Returns a B2BucketReplicationConfiguration for a bucket that will only be a replication destination.
     * sourceToDestinationKeyMapping must not be null or empty.
     * @throws IllegalArgumentException if sourceToDestinationKeyMapping is null or empty
     */
    public static B2BucketReplicationConfiguration createForDestination(Map<String, String> sourceToDestinationKeyMapping) {
        return new B2BucketReplicationConfiguration(
                null,
                new B2DestinationConfig(sourceToDestinationKeyMapping)
        );
    }

    public String getSourceApplicationKeyIdOrNull() {
        return asReplicationSource == null ? null : asReplicationSource.sourceApplicationKeyId;
    }

    public List<B2ReplicationRule> getReplicationRulesOrNull() {
        return asReplicationSource == null ? null : asReplicationSource.replicationRules;
    }

    public Map<String, String> getSourceToDestinationKeyMappingOrNull() {
        return asReplicationDestination == null ? null : asReplicationDestination.sourceToDestinationKeyMapping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2BucketReplicationConfiguration that = (B2BucketReplicationConfiguration) o;
        return Objects.equals(asReplicationSource, that.asReplicationSource) &&
                Objects.equals(asReplicationDestination, that.asReplicationDestination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asReplicationSource, asReplicationDestination);
    }

    @Override
    public String toString() {
        return "B2BucketReplicationConfiguration{" +
                "asReplicationSource=" + asReplicationSource +
                ", asReplicationDestination=" + asReplicationDestination +
                '}';
    }
    
    private static class B2SourceConfig {
        @B2Json.required
        private final String sourceApplicationKeyId;

        @B2Json.required
        private final List<B2ReplicationRule> replicationRules;

        @B2Json.constructor(params = "sourceApplicationKeyId, replicationRules")
        private B2SourceConfig(String sourceApplicationKeyId,
                               List<B2ReplicationRule> replicationRules) {
            B2Preconditions.checkArgumentIsNotNull(replicationRules, "replicationRules");
            B2Preconditions.checkArgument(!replicationRules.isEmpty(), "replicationRules is empty");

            this.sourceApplicationKeyId = sourceApplicationKeyId;
            this.replicationRules = replicationRules;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            B2SourceConfig that = (B2SourceConfig) o;
            return Objects.equals(sourceApplicationKeyId, that.sourceApplicationKeyId) &&
                    Objects.equals(replicationRules, that.replicationRules);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceApplicationKeyId, replicationRules);
        }

        @Override
        public String toString() {
            return "B2SourceConfig{" +
                    "sourceApplicationKeyId='" + sourceApplicationKeyId + '\'' +
                    ", replicationRules=" + replicationRules +
                    '}';
        }
    }
    
    private static class B2DestinationConfig {
        @B2Json.required
        private final Map<String, String> sourceToDestinationKeyMapping;

        @B2Json.constructor(params = "sourceToDestinationKeyMapping")
        private B2DestinationConfig(Map<String, String> sourceToDestinationKeyMapping) {
            B2Preconditions.checkArgumentIsNotNull(
                    sourceToDestinationKeyMapping,
                    "sourceToDestinationKeyMapping"
            );
            B2Preconditions.checkArgument(
                    !sourceToDestinationKeyMapping.isEmpty(),
                    "sourceToDestinationKeyMapping is empty"
            );

            this.sourceToDestinationKeyMapping = sourceToDestinationKeyMapping;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            B2DestinationConfig that = (B2DestinationConfig) o;
            return Objects.equals(sourceToDestinationKeyMapping, that.sourceToDestinationKeyMapping);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceToDestinationKeyMapping);
        }

        @Override
        public String toString() {
            return "B2DestinationConfig{" +
                    "sourceToDestinationKeyMapping=" + sourceToDestinationKeyMapping +
                    '}';
        }
    }
}
