/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.B2TestHelpers;
import com.backblaze.b2.client.exceptions.B2ForbiddenException;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.util.B2Collections.listOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class B2BucketTest extends B2BaseTest {
    private static final B2AccountAuthorization ACCOUNT_AUTH = B2TestHelpers.makeAuth(1);
    private static final String ACCOUNT_ID = ACCOUNT_AUTH.getAccountId();
    private static final String BUCKET_NAME = "bucket1";
    private static final String BUCKET_TYPE = B2BucketTypes.ALL_PUBLIC;
    private static final String FILE_PREFIX = "files/";
    private final Map<String, String> bucketInfo;
    private final List<B2CorsRule> b2CorsRules;
    private final List<B2LifecycleRule> lifecycleRules;
    private final Set<String> optionsSet;
    private final List<B2ReplicationRule> replicationRules;
    private final Map<String, String> sourceToDestinationKeyMapping;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public B2BucketTest() {
        bucketInfo = B2Collections.mapOf(
                "one", "1",
                "two", "2"
        );
        b2CorsRules = new ArrayList<>();
        b2CorsRules.add(B2TestHelpers.makeCorsRule());
        lifecycleRules = listOf(
                B2LifecycleRule.builder(FILE_PREFIX).build()
        );
        optionsSet = B2TestHelpers.makeBucketOrApplicationKeyOptions();

        replicationRules = B2Collections.listOf(
                new B2ReplicationRule(
                        "my-replication-rule",
                        "000011112222333344445555",
                        3,
                        "",
                        false,
                        true
                ),
                new B2ReplicationRule(
                        "my-replication-rule-2",
                        "777011112222333344445555",
                        1,
                        "abc",
                        true,
                        false
                )
        );

        sourceToDestinationKeyMapping = new TreeMap<>();
        sourceToDestinationKeyMapping.put(
                "123a0a1a2a3a4a50000bc614e", "555a0a1a2a3a4a70000bc929a"
        );
        sourceToDestinationKeyMapping.put(
                "456a0b9a8a7a6a50000fc614e", "555a0a1a2a3a4a70000bc929a"
        );
    }

    @Test
    public void testEquals() {
        final B2Bucket bucket = new B2Bucket(
                ACCOUNT_ID,
                bucketId(1),
                BUCKET_NAME,
                BUCKET_TYPE,
                bucketInfo,
                b2CorsRules,
                lifecycleRules,
                optionsSet,
                new B2AuthorizationFilteredResponseField<>(false, null),
                new B2AuthorizationFilteredResponseField<>(false, null),
                new B2AuthorizationFilteredResponseField<>(false, null),
                1
        );
        final String bucketJson = B2Json.toJsonOrThrowRuntime(bucket);
        final B2Bucket convertedBucket = B2Json.fromJsonOrThrowRuntime(bucketJson, B2Bucket.class);
        assertEquals(bucket, convertedBucket);
    }

    @Test
    public void testJsonRoundTrip() {
        final B2AuthorizationFilteredResponseField<B2BucketFileLockConfiguration> bucketFileLockContainer =
                new B2AuthorizationFilteredResponseField<>(
                        true,
                        new B2BucketFileLockConfiguration(
                                true,
                                B2FileRetentionMode.GOVERNANCE,
                                7,
                                B2FileRetentionPeriodUnit.DAYS
                        ));
        final B2AuthorizationFilteredResponseField<B2BucketServerSideEncryption> bucketSseContainer =
                new B2AuthorizationFilteredResponseField<>(
                        true,
                        B2BucketServerSideEncryption.createSseB2Aes256()
                );
        final B2AuthorizationFilteredResponseField<B2BucketReplicationConfiguration> bucketReplicationConfigurationContainer =
                new B2AuthorizationFilteredResponseField<>(
                        true,
                        B2BucketReplicationConfiguration.createForSourceAndDestination(
                                "123a0a1a2a3a4a50000bc614e",
                                replicationRules,
                                sourceToDestinationKeyMapping
                        )
                );
        final B2Bucket bucket = new B2Bucket(
                ACCOUNT_ID,
                bucketId(1),
                BUCKET_NAME,
                BUCKET_TYPE,
                bucketInfo,
                b2CorsRules,
                lifecycleRules,
                optionsSet,
                bucketFileLockContainer,
                bucketSseContainer,
                bucketReplicationConfigurationContainer,
                1
        );
        final String bucketJson = B2Json.toJsonOrThrowRuntime(bucket);
        final B2Bucket convertedBucket = B2Json.fromJsonOrThrowRuntime(bucketJson, B2Bucket.class);
        assertEquals(bucket, convertedBucket);
    }

    @Test
    public void testMinimumJson() {
        final String json = "{\n" +
                "  \"accountId\": \"" + ACCOUNT_ID + "\",\n" +
                "  \"bucketId\": \"" + bucketId(1) + "\",\n" +
                "  \"bucketName\": \"" + BUCKET_NAME + "\",\n" +
                "  \"defaultServerSideEncryption\": {\n" +
                "    \"isClientAuthorizedToRead\": false,\n" +
                "    \"value\": null\n" +
                "  },\n" +
                "  \"fileLockConfiguration\": {\n" +
                "    \"isClientAuthorizedToRead\": false,\n" +
                "    \"value\": null\n" +
                "  },\n" +
                "  \"replicationConfiguration\": {\n" +
                "    \"isClientAuthorizedToRead\": false,\n" +
                "    \"value\": null\n" +
                "  },\n" +
                "  \"revision\": 1\n" +
                "}";

        // Convert from json -> B2Bucket
        final B2Bucket convertedBucket = B2Json.fromJsonOrThrowRuntime(json, B2Bucket.class);

        final B2Bucket bucket = new B2Bucket(
                ACCOUNT_ID,
                bucketId(1),
                BUCKET_NAME,
                null,
                null,
                null,
                null,
                null,
                new B2AuthorizationFilteredResponseField<>(false, null),
                new B2AuthorizationFilteredResponseField<>(false, null),
                new B2AuthorizationFilteredResponseField<>(false, null),
                1
        );

        assertEquals(bucket, convertedBucket);
    }

    @Test
    public void testFromJson() {
        final B2AuthorizationFilteredResponseField<B2BucketFileLockConfiguration> bucketFileLockContainer =
                new B2AuthorizationFilteredResponseField<>(
                        true,
                        new B2BucketFileLockConfiguration(
                                true,
                                B2FileRetentionMode.GOVERNANCE,
                                7,
                                B2FileRetentionPeriodUnit.DAYS
                        ));
        final B2AuthorizationFilteredResponseField<B2BucketServerSideEncryption> bucketSseContainer =
                new B2AuthorizationFilteredResponseField<>(
                        true,
                        B2BucketServerSideEncryption.createSseB2Aes256()
                );
        final B2AuthorizationFilteredResponseField<B2BucketReplicationConfiguration> bucketReplicationConfigurationContainer =
                new B2AuthorizationFilteredResponseField<>(
                        true,
                        B2BucketReplicationConfiguration.createForSourceAndDestination(
                                "123a0a1a2a3a4a50000bc614e",
                                replicationRules,
                                sourceToDestinationKeyMapping
                        )
                );
        final B2Bucket bucket = new B2Bucket(
                ACCOUNT_ID,
                bucketId(1),
                BUCKET_NAME,
                BUCKET_TYPE,
                bucketInfo,
                b2CorsRules,
                lifecycleRules,
                optionsSet,
                bucketFileLockContainer,
                bucketSseContainer,
                bucketReplicationConfigurationContainer,
                1
        );
        // Convert from B2Bucket -> json
        final String bucketJson = B2Json.toJsonOrThrowRuntime(bucket);

        final String json = "{\n" +
                "  \"accountId\": \"1\",\n" +
                "  \"bucketId\": \"bucket1\",\n" +
                "  \"bucketInfo\": {\n" +
                "    \"one\": \"1\",\n" +
                "    \"two\": \"2\"\n" +
                "  },\n" +
                "  \"bucketName\": \"bucket1\",\n" +
                "  \"bucketType\": \"allPublic\",\n" +
                "  \"corsRules\": [\n" +
                "    {\n" +
                "      \"allowedHeaders\": null,\n" +
                "      \"allowedOperations\": [\n" +
                "        \"b2_download_file_by_id\"\n" +
                "      ],\n" +
                "      \"allowedOrigins\": [\n" +
                "        \"https://something.com\"\n" +
                "      ],\n" +
                "      \"corsRuleName\": \"rule-name\",\n" +
                "      \"exposeHeaders\": null,\n" +
                "      \"maxAgeSeconds\": 0\n" +
                "    }\n" +
                "  ],\n" +
                "  \"defaultServerSideEncryption\": {\n" +
                "    \"isClientAuthorizedToRead\": true,\n" +
                "    \"value\": {\n" +
                "      \"algorithm\": \"AES256\",\n" +
                "      \"mode\": \"SSE-B2\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"fileLockConfiguration\": {\n" +
                "    \"isClientAuthorizedToRead\": true,\n" +
                "    \"value\": {\n" +
                "      \"defaultRetention\": {\n" +
                "        \"mode\": \"governance\",\n" +
                "        \"period\": {\n" +
                "          \"duration\": 7,\n" +
                "          \"unit\": \"days\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"isFileLockEnabled\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"lifecycleRules\": [\n" +
                "    {\n" +
                "      \"daysFromHidingToDeleting\": null,\n" +
                "      \"daysFromUploadingToHiding\": null,\n" +
                "      \"fileNamePrefix\": \"files/\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"options\": [\n" +
                "    \"myOption1\",\n" +
                "    \"myOption2\"\n" +
                "  ],\n" +
                "  \"replicationConfiguration\": {\n" +
                "    \"isClientAuthorizedToRead\": true,\n" +
                "    \"value\": {\n" +
                "      \"asReplicationDestination\": {\n" +
                "        \"sourceToDestinationKeyMapping\": {\n" +
                "          \"123a0a1a2a3a4a50000bc614e\": \"555a0a1a2a3a4a70000bc929a\",\n" +
                "          \"456a0b9a8a7a6a50000fc614e\": \"555a0a1a2a3a4a70000bc929a\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"asReplicationSource\": {\n" +
                "        \"replicationRules\": [\n" +
                "          {\n" +
                "            \"destinationBucketId\": \"000011112222333344445555\",\n" +
                "            \"fileNamePrefix\": \"\",\n" +
                "            \"includeExistingFiles\": true,\n" +
                "            \"isEnabled\": false,\n" +
                "            \"priority\": 3,\n" +
                "            \"replicationRuleName\": \"my-replication-rule\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"destinationBucketId\": \"777011112222333344445555\",\n" +
                "            \"fileNamePrefix\": \"abc\",\n" +
                "            \"includeExistingFiles\": false,\n" +
                "            \"isEnabled\": true,\n" +
                "            \"priority\": 1,\n" +
                "            \"replicationRuleName\": \"my-replication-rule-2\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"sourceApplicationKeyId\": \"123a0a1a2a3a4a50000bc614e\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"revision\": 1\n" +
                "}";

        // Convert from json -> B2Bucket
        final B2Bucket convertedBucket = B2Json.fromJsonOrThrowRuntime(json, B2Bucket.class);
        assertEquals(bucket, convertedBucket);

        // Compare json
        assertEquals(json, bucketJson);
    }


    @Test
    public void testUnauthorizedToReadServerSideEncryptionThrowsException() throws B2ForbiddenException {
        final String bucketJson = "{\n" +
                "  \"accountId\": \"1\",\n" +
                "  \"bucketId\": \"bucket1\",\n" +
                "  \"bucketInfo\": {\n" +
                "    \"one\": \"1\",\n" +
                "    \"two\": \"2\"\n" +
                "  },\n" +
                "  \"bucketName\": \"bucket1\",\n" +
                "  \"bucketType\": \"allPublic\",\n" +
                "  \"corsRules\": [\n" +
                "    {\n" +
                "      \"allowedHeaders\": null,\n" +
                "      \"allowedOperations\": [\n" +
                "        \"b2_download_file_by_id\"\n" +
                "      ],\n" +
                "      \"allowedOrigins\": [\n" +
                "        \"https://something.com\"\n" +
                "      ],\n" +
                "      \"corsRuleName\": \"rule-name\",\n" +
                "      \"exposeHeaders\": null,\n" +
                "      \"maxAgeSeconds\": 0\n" +
                "    }\n" +
                "  ],\n" +
                "  \"fileLockConfiguration\": {\n" +
                "    \"isClientAuthorizedToRead\": true,\n" +
                "    \"value\": {\n" +
                "      \"defaultRetention\": {\n" +
                "        \"mode\": \"governance\",\n" +
                "        \"period\": {\n" +
                "          \"duration\": 7,\n" +
                "          \"unit\": \"days\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"isFileLockEnabled\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"defaultServerSideEncryption\": {\n" +
                "    \"isClientAuthorizedToRead\": false,\n" +
                "    \"value\": null\n" +
                "  },\n" +
                "  \"replicationConfiguration\": {\n" +
                "    \"isClientAuthorizedToRead\": true,\n" +
                "    \"value\": {\n" +
                "      \"asReplicationDestination\": {\n" +
                "        \"sourceToDestinationKeyMapping\": {\n" +
                "          \"123a0a1a2a3a4a50000bc614e\": \"555a0a1a2a3a4a70000bc929a\",\n" +
                "          \"456a0b9a8a7a6a50000fc614e\": \"555a0a1a2a3a4a70000bc929a\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"asReplicationSource\": {\n" +
                "        \"replicationRules\": [\n" +
                "          {\n" +
                "            \"destinationBucketId\": \"000011112222333344445555\",\n" +
                "            \"fileNamePrefix\": \"\",\n" +
                "            \"includeExistingFiles\": true,\n" +
                "            \"isEnabled\": false,\n" +
                "            \"priority\": 3,\n" +
                "            \"replicationRuleName\": \"my-replication-rule\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"destinationBucketId\": \"777011112222333344445555\",\n" +
                "            \"fileNamePrefix\": \"abc\",\n" +
                "            \"includeExistingFiles\": false,\n" +
                "            \"isEnabled\": true,\n" +
                "            \"priority\": 1,\n" +
                "            \"replicationRuleName\": \"my-replication-rule-2\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"sourceApplicationKeyId\": \"123a0a1a2a3a4a50000bc614e\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"lifecycleRules\": [\n" +
                "    {\n" +
                "      \"daysFromHidingToDeleting\": null,\n" +
                "      \"daysFromUploadingToHiding\": null,\n" +
                "      \"fileNamePrefix\": \"files/\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"options\": [\n" +
                "    \"myOption1\",\n" +
                "    \"myOption2\"\n" +
                "  ],\n" +
                "  \"revision\": 1\n" +
                "}";
        final B2Bucket bucket = B2Json.fromJsonOrThrowRuntime(bucketJson, B2Bucket.class);

        assertFalse(bucket.isClientAuthorizedToReadDefaultServerSideEncryption());

        thrown.expect(B2ForbiddenException.class);

        // The following must throw
        bucket.getDefaultServerSideEncryption();
    }

    @Test
    public void testUnauthorizedToReadReplicationConfigurationThrowsException() throws B2ForbiddenException {
        final String bucketJson = "{\n" +
                "  \"accountId\": \"1\",\n" +
                "  \"bucketId\": \"bucket1\",\n" +
                "  \"bucketInfo\": {\n" +
                "    \"one\": \"1\",\n" +
                "    \"two\": \"2\"\n" +
                "  },\n" +
                "  \"bucketName\": \"bucket1\",\n" +
                "  \"bucketType\": \"allPublic\",\n" +
                "  \"corsRules\": [\n" +
                "    {\n" +
                "      \"allowedHeaders\": null,\n" +
                "      \"allowedOperations\": [\n" +
                "        \"b2_download_file_by_id\"\n" +
                "      ],\n" +
                "      \"allowedOrigins\": [\n" +
                "        \"https://something.com\"\n" +
                "      ],\n" +
                "      \"corsRuleName\": \"rule-name\",\n" +
                "      \"exposeHeaders\": null,\n" +
                "      \"maxAgeSeconds\": 0\n" +
                "    }\n" +
                "  ],\n" +
                "  \"defaultServerSideEncryption\": {\n" +
                "    \"isClientAuthorizedToRead\": true,\n" +
                "    \"value\": {\n" +
                "      \"algorithm\": \"AES256\",\n" +
                "      \"mode\": \"SSE-B2\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"fileLockConfiguration\": {\n" +
                "    \"isClientAuthorizedToRead\": true,\n" +
                "    \"value\": {\n" +
                "      \"defaultRetention\": {\n" +
                "        \"mode\": \"governance\",\n" +
                "        \"period\": {\n" +
                "          \"duration\": 7,\n" +
                "          \"unit\": \"days\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"isFileLockEnabled\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"lifecycleRules\": [\n" +
                "    {\n" +
                "      \"daysFromHidingToDeleting\": null,\n" +
                "      \"daysFromUploadingToHiding\": null,\n" +
                "      \"fileNamePrefix\": \"files/\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"options\": [\n" +
                "    \"myOption1\",\n" +
                "    \"myOption2\"\n" +
                "  ],\n" +
                "  \"replicationConfiguration\": {\n" +
                "    \"isClientAuthorizedToRead\": false,\n" +
                "    \"value\": null\n" +
                "  },\n" +
                "  \"revision\": 1\n" +
                "}";

        final B2Bucket bucket = B2Json.fromJsonOrThrowRuntime(bucketJson, B2Bucket.class);

        assertFalse(bucket.isClientAuthorizedToReadReplicationConfiguration());

        thrown.expect(B2ForbiddenException.class);

        // The following must throw
        bucket.getReplicationConfiguration();
    }
}