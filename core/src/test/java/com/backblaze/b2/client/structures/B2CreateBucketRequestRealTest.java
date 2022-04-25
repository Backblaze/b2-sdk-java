/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.B2TestHelpers;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.backblaze.b2.util.B2Collections.listOf;
import static com.backblaze.b2.util.B2Collections.mapOf;
import static org.junit.Assert.assertEquals;

public class B2CreateBucketRequestRealTest extends B2BaseTest {
    private static final B2AccountAuthorization ACCOUNT_AUTH = B2TestHelpers.makeAuth(1);
    private static final String ACCOUNT_ID = ACCOUNT_AUTH.getAccountId();
    private static final String BUCKET_NAME = "bucket1";
    private static final String BUCKET_TYPE = B2BucketTypes.ALL_PUBLIC;
    private static final String FILE_PREFIX = "files/";

    @Test
    public void testMinimalConfig() {
        final String configJson = "{\n" +
                "   \"accountId\": \"" + ACCOUNT_ID + "\",\n" +
                "   \"bucketName\": \"" + BUCKET_NAME + "\",\n" +
                "   \"bucketType\": \"" + BUCKET_TYPE + "\"\n" +
                "}";
        final B2CreateBucketRequestReal convertedRequest = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2CreateBucketRequestReal.class
        );

        final B2CreateBucketRequestReal defaultRequest =
                new B2CreateBucketRequestReal(
                        ACCOUNT_ID,
                        B2CreateBucketRequest
                                .builder(BUCKET_NAME, BUCKET_TYPE)
                                .build()
                );

        assertEquals(defaultRequest, convertedRequest);
    }

    @Test
    public void testFullCreateBucketRequestReal() {
        final Map<String, String> bucketInfo = B2Collections.mapOf(
                "one", "1",
                "two", "2"
        );
        final List<B2CorsRule> b2CorsRules = listOf(B2TestHelpers.makeCorsRule());
        final List<B2LifecycleRule> lifecycleRules = listOf(
                B2LifecycleRule.builder(FILE_PREFIX).build()
        );
        final B2BucketServerSideEncryption defaultServerSideEncryption =
                B2BucketServerSideEncryption.createSseB2Aes256();

        final List<B2ReplicationRule> replicationRules = listOf(
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
        final Map<String, String> sourceToDestinationKeyMapping = mapOf(
                "123a0a1a2a3a4a50000bc614e", "555a0a1a2a3a4a70000bc929a",
                "456a0b9a8a7a6a50000fc614e", "555a0a1a2a3a4a70000bc929a"
        );

        final B2BucketReplicationConfiguration replicationConfiguration =
                B2BucketReplicationConfiguration.createForSourceAndDestination(
                        "123a0a1a2a3a4a50000bc614e",
                        replicationRules,
                        sourceToDestinationKeyMapping
                );

        final B2CreateBucketRequestReal createRequestReal =
                new B2CreateBucketRequestReal(
                        ACCOUNT_ID,
                        B2CreateBucketRequest
                                .builder(BUCKET_NAME, BUCKET_TYPE)
                                .setBucketInfo(bucketInfo)
                                .setCorsRules(b2CorsRules)
                                .setLifecycleRules(lifecycleRules)
                                .setFileLockEnabled(true)
                                .setDefaultServerSideEncryption(defaultServerSideEncryption)
                                .setReplicationConfiguration(replicationConfiguration)
                                .build()
                );

        // Convert from B2CreateBucketRequestReal -> json
        final String requestJson = B2Json.toJsonOrThrowRuntime(createRequestReal);

        final String json =  "{\n" +
                "  \"accountId\": \"1\",\n" +
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
                "    \"algorithm\": \"AES256\",\n" +
                "    \"mode\": \"SSE-B2\"\n" +
                "  },\n" +
                "  \"fileLockEnabled\": true,\n" +
                "  \"lifecycleRules\": [\n" +
                "    {\n" +
                "      \"daysFromHidingToDeleting\": null,\n" +
                "      \"daysFromUploadingToHiding\": null,\n" +
                "      \"fileNamePrefix\": \"files/\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"replicationConfiguration\": {\n" +
                "    \"asReplicationDestination\": {\n" +
                "      \"sourceToDestinationKeyMapping\": {\n" +
                "        \"123a0a1a2a3a4a50000bc614e\": \"555a0a1a2a3a4a70000bc929a\",\n" +
                "        \"456a0b9a8a7a6a50000fc614e\": \"555a0a1a2a3a4a70000bc929a\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"asReplicationSource\": {\n" +
                "      \"replicationRules\": [\n" +
                "        {\n" +
                "          \"destinationBucketId\": \"000011112222333344445555\",\n" +
                "          \"fileNamePrefix\": \"\",\n" +
                "          \"includeExistingFiles\": true,\n" +
                "          \"isEnabled\": false,\n" +
                "          \"priority\": 3,\n" +
                "          \"replicationRuleName\": \"my-replication-rule\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"destinationBucketId\": \"777011112222333344445555\",\n" +
                "          \"fileNamePrefix\": \"abc\",\n" +
                "          \"includeExistingFiles\": false,\n" +
                "          \"isEnabled\": true,\n" +
                "          \"priority\": 1,\n" +
                "          \"replicationRuleName\": \"my-replication-rule-2\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"sourceApplicationKeyId\": \"123a0a1a2a3a4a50000bc614e\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        // Convert from json -> B2CreateBucketRequestReal
        final B2CreateBucketRequestReal convertedRequest =
                B2Json.fromJsonOrThrowRuntime(
                        json,
                        B2CreateBucketRequestReal.class
                );

        // Compare json
        assertEquals(json, requestJson);

        // Compare requests
        assertEquals(createRequestReal, convertedRequest);
    }
}