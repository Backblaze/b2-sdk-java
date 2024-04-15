/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static org.junit.Assert.assertEquals;

public class B2GetBucketNotificationRulesRequestTest extends B2BaseTest {

    private static final String BUCKET_ID = bucketId(1);

    @Test
    public void testFullGetBucketNotificationRulesRequest() {

        final B2GetBucketNotificationRulesRequest b2GetBucketNotificationRulesRequest =
                B2GetBucketNotificationRulesRequest.builder(BUCKET_ID)
                .build();

        // Convert from B2GetBucketNotificationRulesRequest -> json
        final String requestJson = B2Json.toJsonOrThrowRuntime(b2GetBucketNotificationRulesRequest);

        final String json =  "{\n" +
                "  \"bucketId\": \"" + BUCKET_ID + "\"\n" +
                "}";

        // Convert from json -> B2GetBucketNotificationRulesRequest
        final B2GetBucketNotificationRulesRequest convertedRequest = B2Json.fromJsonOrThrowRuntime(json, B2GetBucketNotificationRulesRequest.class);

        // Compare json
        assertEquals(json, requestJson);

        // Compare requests
        assertEquals(b2GetBucketNotificationRulesRequest, convertedRequest);

    }
}