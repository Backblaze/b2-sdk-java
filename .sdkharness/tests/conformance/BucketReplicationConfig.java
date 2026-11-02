// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2BucketReplicationConfiguration;
import com.backblaze.b2.client.structures.B2CreateKeyRequest;
import com.backblaze.b2.client.structures.B2CreatedApplicationKey;
import com.backblaze.b2.client.structures.B2ReplicationRule;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * bucket.replication_config. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_update_bucket takes replicationConfiguration; "At least one of the asReplicationSource or
 * asReplicationDestination parameters is required". Every rule field set here (replicationRuleName,
 * destinationBucketId, fileNamePrefix, includeExistingFiles, isEnabled, priority) plus sourceApplicationKeyId is a
 * documented field, and "Call b2_list_buckets to retrieve all of the replication rules associated with one or more
 * buckets." (https://www.backblaze.com/apidocs/b2-update-bucket,
 * https://www.backblaze.com/docs/cloud-storage-create-a-cloud-replication-rule-with-the-native-api).
 *
 * <p>Two buckets and a replication-capable key (the capabilities the documentation lists for a source) are created
 * and one source rule is set on the first bucket, naming the second as the destination. Asserted: the read-back of
 * the source bucket carries the source key id and the rule, field for field, and no destination side; the
 * destination bucket reports no source configuration. Nothing is replicated: the check only stores and reads back
 * the configuration, which is the whole of this capability (the SDK offers the structures, not a helper that runs
 * replication).
 */
public class BucketReplicationConfig {
    public static void main(String[] args) {
        Support.run("bucket.replication_config", () -> {
            Support.step("buckets and a key");
            final B2Bucket source = Support.bucket();
            final B2Bucket destination = Support.bucket();
            final B2CreatedApplicationKey key = Support.client.createKey(B2CreateKeyRequest.builder(new HashSet<>(Arrays.asList(
                    "listBuckets", "listFiles", "readFiles", "shareFiles", "writeFiles", "deleteFiles",
                    "readFileLegalHolds", "readFileRetentions", "readBucketReplications", "writeBucketReplications")),
                    "sdkharness-" + source.getBucketName().substring(source.getBucketName().length() - 8)).build());
            Support.trackKey(key.getApplicationKeyId());

            Support.step("set a source rule");
            final B2ReplicationRule rule = new B2ReplicationRule("sdkharness-conf", destination.getBucketId(), 1, "st/", true, false);
            Support.client.updateBucket(B2UpdateBucketRequest.builder(source)
                    .setReplicationConfiguration(B2BucketReplicationConfiguration.createForSource(key.getApplicationKeyId(), Collections.singletonList(rule))).build());

            Support.step("read back");
            final B2BucketReplicationConfiguration read = listed(source).getReplicationConfiguration();
            Support.eq("source application key id", key.getApplicationKeyId(), read.getSourceApplicationKeyIdOrNull());
            final List<B2ReplicationRule> rules = read.getReplicationRulesOrNull();
            Support.check(rules != null && rules.size() == 1, "expected one replication rule, got " + (rules == null ? "none" : rules.size()));
            final B2ReplicationRule actual = rules.get(0);
            Support.eq("replicationRuleName", rule.getReplicationRuleName(), actual.getReplicationRuleName());
            Support.eq("destinationBucketId", rule.getDestinationBucketId(), actual.getDestinationBucketId());
            Support.eq("priority", rule.getPriority(), actual.getPriority());
            Support.eq("fileNamePrefix", rule.getFileNamePrefix(), actual.getFileNamePrefix());
            Support.eq("isEnabled", rule.isEnabled(), actual.isEnabled());
            Support.eq("includeExistingFiles", rule.includeExistingFiles(), actual.includeExistingFiles());
            Support.eq("a source-only bucket has no destination mapping", null, read.getSourceToDestinationKeyMappingOrNull());

            Support.step("the destination bucket is not a source");
            final B2BucketReplicationConfiguration other = listed(destination).getReplicationConfiguration();
            Support.check(other == null || other.getReplicationRulesOrNull() == null, "the destination bucket reports replication rules it was never given");
        });
    }

    private static B2Bucket listed(B2Bucket bucket) throws Exception {
        final B2Bucket found = Support.client.getBucketOrNullByName(bucket.getBucketName());
        Support.check(found != null, "the bucket is not listed");
        return found;
    }
}
