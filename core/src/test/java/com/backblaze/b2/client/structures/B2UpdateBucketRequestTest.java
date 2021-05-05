package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.B2TestHelpers;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Collections;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.util.B2Collections.listOf;

/*
 * Copyright 2020, Backblaze, Inc. All rights reserved.
 */
public class B2UpdateBucketRequestTest {

    private static final B2AccountAuthorization ACCOUNT_AUTH = B2TestHelpers.makeAuth(1);
    private static final String ACCOUNT_ID = ACCOUNT_AUTH.getAccountId();
    private static final String BUCKET_NAME = "bucket1";
    private static final String BUCKET_ID = bucketId(1);
    private static final String BUCKET_TYPE = B2BucketTypes.ALL_PUBLIC;
    private static final String FILE_PREFIX = "files/";

    public B2UpdateBucketRequestTest() {

    }

    @Test
    public void testDefaultConfig() {
        final String configJson = "{\n" +
                "   \"accountId\": \"" + ACCOUNT_ID + "\",\n" +
                "   \"bucketId\": \"" + BUCKET_ID + "\"\n," +
                "   \"ifRevisionIs\": 1\n" +
                "}";
        final B2UpdateBucketRequest convertedRequest = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2UpdateBucketRequest.class);

        B2Bucket defaultBucket = new B2Bucket(
                ACCOUNT_ID,
                BUCKET_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1);
        final B2UpdateBucketRequest defaultRequest = B2UpdateBucketRequest.builder(defaultBucket).build();
        Assert.assertEquals(defaultRequest, convertedRequest);
    }

    @Test
    public void testEquals() {
        final B2UpdateBucketRequest config = B2UpdateBucketRequest.builder(makeBucket()).build();
        final String configJson = B2Json.toJsonOrThrowRuntime(config);
        final B2UpdateBucketRequest convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2UpdateBucketRequest.class);
        Assert.assertEquals(config, convertedConfig);
    }

    @Test
    public void testFullUpdateBucketRequest() {

        final Map<String, String> bucketInfo = B2Collections.mapOf(
                "one", "1",
                "two", "2"
        );
        List<B2CorsRule> b2CorsRules = new ArrayList<>();
        b2CorsRules.add(B2TestHelpers.makeCorsRule());
        List<B2LifecycleRule> lifecycleRules = listOf(
                B2LifecycleRule.builder(FILE_PREFIX).build()
        );
        final B2BucketDefaultRetention defaultRetention =
                new B2BucketDefaultRetention(B2FileRetentionMode.GOVERNANCE, 7, B2FileRetentionPeriodUnit.DAYS);
        final B2BucketServerSideEncryption defaultServerSideEncryption =
                B2BucketServerSideEncryption.createSseB2Aes256();

        final B2UpdateBucketRequest updateRequest = B2UpdateBucketRequest.builder(
                new B2Bucket(
                        ACCOUNT_ID,
                        bucketId(1),
                        BUCKET_NAME,
                        BUCKET_TYPE,
                        bucketInfo,
                        b2CorsRules,
                        lifecycleRules,
                        null,
                        null,
                        null,
                        1
                ))
                .setBucketInfo(bucketInfo)
                .setBucketType(BUCKET_TYPE)
                .setCorsRules(b2CorsRules)
                .setLifecycleRules(lifecycleRules)
                .setDefaultRetention(defaultRetention)
                .setDefaultServerSideEncryption(defaultServerSideEncryption)
                .build();

        // Convert from B2UpdateBucketRequest -> json
        final String requestJson = B2Json.toJsonOrThrowRuntime(updateRequest);

        final String json =  "{\n" +
                "  \"accountId\": \"1\",\n" +
                "  \"bucketId\": \"bucket1\",\n" +
                "  \"bucketInfo\": {\n" +
                "    \"one\": \"1\",\n" +
                "    \"two\": \"2\"\n" +
                "  },\n" +
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
                "  \"defaultRetention\": {\n" +
                "    \"mode\": \"governance\",\n" +
                "    \"period\": {\n" +
                "      \"duration\": 7,\n" +
                "      \"unit\": \"days\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"defaultServerSideEncryption\": {\n" +
                "    \"algorithm\": \"AES256\",\n" +
                "    \"mode\": \"SSE-B2\"\n" +
                "  },\n" +
                "  \"ifRevisionIs\": 1,\n" +
                "  \"lifecycleRules\": [\n" +
                "    {\n" +
                "      \"daysFromHidingToDeleting\": null,\n" +
                "      \"daysFromUploadingToHiding\": null,\n" +
                "      \"fileNamePrefix\": \"files/\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Convert from json -> B2UpdateBucketRequest
        final B2UpdateBucketRequest convertedRequest = B2Json.fromJsonOrThrowRuntime(json, B2UpdateBucketRequest.class);

        // Compare json
        Assert.assertEquals(json, requestJson);

        // Compare requests
        Assert.assertEquals(updateRequest, convertedRequest);

    }

    private B2Bucket makeBucket() {
        return new B2Bucket(
                ACCOUNT_ID,
                BUCKET_ID,
                BUCKET_NAME,
                BUCKET_TYPE,
                null,
                null,
                null,
                null,
                null,
                null,
                1);
    }

}