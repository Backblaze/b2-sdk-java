/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.List;
import java.util.TreeSet;

import static com.backblaze.b2.client.B2TestHelpers.bucketId;
import static com.backblaze.b2.util.B2Collections.listOf;
import static org.junit.Assert.assertEquals;

public class B2SetBucketNotificationRulesResponseTest extends B2BaseTest {

    private static final String BUCKET_ID = bucketId(1);

    @Test
    public void testFullSetBucketNotificationRulesResponse() {

        final List<B2EventNotificationRule> eventNotificationRules = listOf(
                new B2EventNotificationRule(
                        "ruleName",
                        new TreeSet<>(listOf("b2:ObjectCreated:Copy")),
                        "",
                        new B2WebhookConfiguration("https://www.example.com"),
                        true,
                        ""
                )
        );

        final B2SetBucketNotificationRulesResponse b2SetBucketNotificationRulesResponse =
                B2SetBucketNotificationRulesResponse.builder(BUCKET_ID, eventNotificationRules)
                        .build();

        // Convert from B2SetBucketNotificationRulesResponse -> json
        final String requestJson = B2Json.toJsonOrThrowRuntime(b2SetBucketNotificationRulesResponse);

        final String json =  "{\n" +
                "  \"bucketId\": \"" + BUCKET_ID + "\",\n" +
                "  \"eventNotificationRules\": [\n" +
                "    {\n" +
                "      \"disabledReason\": \"\",\n" +
                "      \"eventTypes\": [\n" +
                "        \"b2:ObjectCreated:Copy\"\n" +
                "      ],\n" +
                "      \"isEnabled\": true,\n" +
                "      \"name\": \"ruleName\",\n" +
                "      \"objectNamePrefix\": \"\",\n" +
                "      \"targetConfiguration\": {\n" +
                "        \"targetType\": \"webhook\",\n" +
                "        \"url\": \"https://www.example.com\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Convert from json -> B2SetBucketNotificationRulesResponse
        final B2SetBucketNotificationRulesResponse convertedResponse = B2Json.fromJsonOrThrowRuntime(json, B2SetBucketNotificationRulesResponse.class);

        // Compare json
        assertEquals(json, requestJson);

        // Compare requests
        assertEquals(b2SetBucketNotificationRulesResponse, convertedResponse);

    }
}