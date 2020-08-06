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
                1,
                false);
        final String bucketJson = B2Json.toJsonOrThrowRuntime(bucket);
        final B2Bucket convertedBucket = B2Json.fromJsonOrThrowRuntime(bucketJson, B2Bucket.class);
        assertEquals(bucket, convertedBucket);
    }
}
