/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

/**
 * One rule about how files should be replicated
 * to the destination bucket.
 */
public class B2ReplicationRule {
    /**
     * A name for identifying the rule. Names must be unique within a bucket.
     */
    @B2Json.required
    private final String replicationRuleName;

    /**
     * The ID of the destination bucket.  Always set.
     */
    @B2Json.required
    private final String destinationBucketId;

    /**
     * The priority of this replication rule as compared to any others defined for the bucket.  When two or more
     * rules have the same destination, the highest priority value rule will win.
     *
     * Note that priority will take effect only if two or more rules match a particular file.
     * For example, if there are 2 rules with fileNamePrefixes "star" and "starlord", both with destination
     * bucket B and a file named "starlord - Guardians of the Galaxy" is being replicated, then
     * priority will be used to select the rule to use. Currently this only comes into play for
     * includeExistingFiles where one rule might have that value set to true and another might have it set to false
     * (e.g. they only want existing files matching a very specific fileNamePrefix to be replicated).
     */
    @B2Json.required
    private final int priority;

    /**
     * A filtering rule restricting this replication rule to files with a filename that begins with the
     * specified prefix.
     * Always set. "" means all files.
     */
    @B2Json.required
    private final String fileNamePrefix;

    /**
     * Indicates if the rule is enabled.
     */
    @B2Json.required
    private final boolean isEnabled;

    /**
     * Indicates if existing files in the bucket will be replicated (if they have not already been replicated
     * to the destination bucket this rule specifies).
     */
    @B2Json.required
    private final boolean includeExistingFiles;

    /**
     * Initializes a new, immutable rule.
     */
    @B2Json.constructor(params = "replicationRuleName, destinationBucketId, priority, " +
        "fileNamePrefix, isEnabled, includeExistingFiles")
    public B2ReplicationRule(String replicationRuleName,
                             String destinationBucketId,
                             int priority,
                             String fileNamePrefix,
                             boolean isEnabled,
                             boolean includeExistingFiles) {
        this.replicationRuleName = replicationRuleName;
        this.destinationBucketId = destinationBucketId;
        this.priority = priority;
        this.fileNamePrefix = fileNamePrefix;
        this.isEnabled = isEnabled;
        this.includeExistingFiles = includeExistingFiles;
    }

    public String getReplicationRuleName() {
        return replicationRuleName;
    }

    public String getDestinationBucketId() {
        return destinationBucketId;
    }

    public int getPriority() {
        return priority;
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean includeExistingFiles() {
        return includeExistingFiles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2ReplicationRule that = (B2ReplicationRule) o;
        return priority == that.priority &&
                isEnabled == that.isEnabled &&
                includeExistingFiles == that.includeExistingFiles &&
                Objects.equals(replicationRuleName, that.replicationRuleName) &&
                Objects.equals(destinationBucketId, that.destinationBucketId) &&
                Objects.equals(fileNamePrefix, that.fileNamePrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                replicationRuleName,
                destinationBucketId,
                priority,
                fileNamePrefix,
                isEnabled,
                includeExistingFiles
        );
    }

    @Override
    public String toString() {
        return "B2ReplicationRule{" +
                "replicationRuleName='" + replicationRuleName + '\'' +
                ", destinationBucketId='" + destinationBucketId + '\'' +
                ", priority=" + priority +
                ", fileNamePrefix='" + fileNamePrefix + '\'' +
                ", isEnabled=" + isEnabled +
                ", includeExistingFiles=" + includeExistingFiles +
                '}';
    }
}
