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

public class B2GetBucketNotificationRulesResponseTest extends B2BaseTest {

    private static final String BUCKET_ID = bucketId(1);

    @Test
    public void testFullGetBucketNotificationRulesResponse() {

        final List<B2EventNotificationRuleForResponse> eventNotificationRuleForResponseList = listOf(
                new B2EventNotificationRuleForResponse(
                        "ruleName",
                        new TreeSet<>(listOf("b2:ObjectCreated:Copy")),
                        "",
                        new B2WebhookConfigurationForResponse(
                                "https://www.example.com",
                                new TreeSet<>(
                                        listOf(
                                                new B2CustomHeaderForResponse("name1", "val1"),
                                                new B2CustomHeaderForResponse("name2", "val2")
                                        )
                                ),
                                "dummySigningSecret"),
                        true,
                        false,
                        null
                )
        );

        final B2GetBucketNotificationRulesResponse b2GetBucketNotificationRulesResponse =
                B2GetBucketNotificationRulesResponse.builder(BUCKET_ID, eventNotificationRuleForResponseList)
                        .build();

        // Convert from B2GetBucketNotificationRulesResponse -> json
        final String requestJson = B2Json.toJsonOrThrowRuntime(b2GetBucketNotificationRulesResponse);

        final String json =  "{\n" +
                "  \"bucketId\": \"" + BUCKET_ID + "\",\n" +
                "  \"eventNotificationRules\": [\n" +
                "    {\n" +
                "      \"eventTypes\": [\n" +
                "        \"b2:ObjectCreated:Copy\"\n" +
                "      ],\n" +
                "      \"isEnabled\": true,\n" +
                "      \"isSuspended\": false,\n" +
                "      \"name\": \"ruleName\",\n" +
                "      \"objectNamePrefix\": \"\",\n" +
                "      \"suspensionReason\": null,\n" +
                "      \"targetConfiguration\": {\n" +
                "        \"customHeaders\": [\n" +
                "          {\n" +
                "            \"name\": \"name1\",\n" +
                "            \"value\": \"val1\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"name\": \"name2\",\n" +
                "            \"value\": \"val2\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"hmacSha256SigningSecret\": \"dummySigningSecret\",\n" +
                "        \"targetType\": \"webhook\",\n" +
                "        \"url\": \"https://www.example.com\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Convert from json -> B2GetBucketNotificationRulesResponse
        final B2GetBucketNotificationRulesResponse convertedResponse = B2Json.fromJsonOrThrowRuntime(json, B2GetBucketNotificationRulesResponse.class);

        // Compare json
        assertEquals(json, requestJson);

        // Compare requests
        assertEquals(b2GetBucketNotificationRulesResponse, convertedResponse);

    }
}