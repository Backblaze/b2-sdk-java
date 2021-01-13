/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.B2TestHelpers;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Collections;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.util.B2Collections.listOf;
import static org.junit.Assert.assertEquals;

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
                null,
                null,
                1);
        final String bucketJson = B2Json.toJsonOrThrowRuntime(bucket);
        final B2Bucket convertedBucket = B2Json.fromJsonOrThrowRuntime(bucketJson, B2Bucket.class);
        assertEquals(bucket, convertedBucket);
    }

    @Test
    public void testJsonRoundTrip() {
        final B2Bucket bucket = new B2Bucket(
                ACCOUNT_ID,
                bucketId(1),
                BUCKET_NAME,
                BUCKET_TYPE,
                bucketInfo,
                b2CorsRules,
                lifecycleRules,
                optionsSet,
                new B2BucketFileLockConfiguration(
                        "enabled",
                        new B2BucketFileLockPeriod(7, "days"),
                        "governance"),
                new B2BucketServerSideEncryption("SSE-B2", "AES256"),
                1);
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
                null,
                null,
                1);

        assertEquals(bucket, convertedBucket);
    }

    @Test
    public void testFromJson() {
        final B2Bucket bucket = new B2Bucket(
                ACCOUNT_ID,
                bucketId(1),
                BUCKET_NAME,
                BUCKET_TYPE,
                bucketInfo,
                b2CorsRules,
                lifecycleRules,
                optionsSet,
                new B2BucketFileLockConfiguration(
                        "enabled",
                        new B2BucketFileLockPeriod(7, "days"),
                        "governance"),
                new B2BucketServerSideEncryption("SSE-B2", "AES256"),
                1);
        // Convert from B2Bucket -> json
        final String bucketJson = B2Json.toJsonOrThrowRuntime(bucket);

        final String json =  "{\n" +
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
                "  \"defaultFileLockConfiguration\": {\n" +
                "    \"mode\": \"governance\",\n" +
                "    \"period\": {\n" +
                "      \"duration\": 7,\n" +
                "      \"unit\": \"days\"\n" +
                "    },\n" +
                "    \"status\": \"enabled\"\n" +
                "  },\n" +
                "  \"defaultServerSideEncryption\": {\n" +
                "    \"algorithm\": \"AES256\",\n" +
                "    \"mode\": \"SSE-B2\"\n" +
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

        // Convert from json -> B2Bucket
        final B2Bucket convertedBucket = B2Json.fromJsonOrThrowRuntime(json, B2Bucket.class);

        // Compare json
        assertEquals(json, bucketJson);
    }


    @Test
    public void testFromJsonWithoutFileLockConfiguration() {
        final B2Bucket bucket = new B2Bucket(
                ACCOUNT_ID,
                bucketId(1),
                BUCKET_NAME,
                BUCKET_TYPE,
                bucketInfo,
                b2CorsRules,
                lifecycleRules,
                optionsSet,
                null,
                null,
                1);
        // Convert from B2Bucket -> json
        final String bucketJson = B2Json.toJsonOrThrowRuntime(bucket);

        final String json =  "{\n" +
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
                "  \"defaultFileLockConfiguration\": null,\n" +
                "  \"defaultServerSideEncryption\": null,\n" +
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

        // Convert from json -> B2Bucket
        final B2Bucket convertedBucket = B2Json.fromJsonOrThrowRuntime(json, B2Bucket.class);

        // Compare json
        assertEquals(json, bucketJson);
    }

}
