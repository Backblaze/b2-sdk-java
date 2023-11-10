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

public class B2SetBucketNotificationRulesRequestTest extends B2BaseTest {

    private static final String BUCKET_ID = bucketId(1);

    @Test
    public void testFullSetBucketNotificationRulesRequest() {

        final List<B2EventNotificationRuleForRequest> eventNotificationRuleForRequestList = listOf(
                new B2EventNotificationRuleForRequest(
                        "ruleName",
                        new TreeSet<>(listOf("b2:ObjectCreated:Copy")),
                        "",
                        new B2WebhookConfigurationForRequest("https://www.example.com"),
                        true
                ),
                new B2EventNotificationRuleForRequest(
                        "ruleNameWithCustomHeaders",
                        new TreeSet<>(listOf("b2:ObjectCreated:Replica")),
                        "",
                        new B2WebhookConfigurationForRequest(
                                "https://www.example.com",
                                new TreeSet<>(
                                        listOf(
                                                new B2CustomHeaderForRequest("name1", "val1"),
                                                new B2CustomHeaderForRequest("name2", "val2")
                                        )
                                )
                        ),
                        true
                )
        );

        final B2SetBucketNotificationRulesRequest b2SetBucketNotificationRulesRequest =
                B2SetBucketNotificationRulesRequest.builder(BUCKET_ID, eventNotificationRuleForRequestList)
                .build();

        // Convert from B2SetBucketNotificationRulesRequest -> json
        final String requestJson = B2Json.toJsonOrThrowRuntime(b2SetBucketNotificationRulesRequest);

        final String json =  "{\n" +
                "  \"bucketId\": \"" + BUCKET_ID + "\",\n" +
                "  \"eventNotificationRules\": [\n" +
                "    {\n" +
                "      \"eventTypes\": [\n" +
                "        \"b2:ObjectCreated:Copy\"\n" +
                "      ],\n" +
                "      \"isEnabled\": true,\n" +
                "      \"name\": \"ruleName\",\n" +
                "      \"objectNamePrefix\": \"\",\n" +
                "      \"targetConfiguration\": {\n" +
                "        \"customHeaders\": null,\n" +
                "        \"targetType\": \"webhook\",\n" +
                "        \"url\": \"https://www.example.com\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"eventTypes\": [\n" +
                "        \"b2:ObjectCreated:Replica\"\n" +
                "      ],\n" +
                "      \"isEnabled\": true,\n" +
                "      \"name\": \"ruleNameWithCustomHeaders\",\n" +
                "      \"objectNamePrefix\": \"\",\n" +
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
                "        \"targetType\": \"webhook\",\n" +
                "        \"url\": \"https://www.example.com\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Convert from json -> B2SetBucketNotificationRulesRequest
        final B2SetBucketNotificationRulesRequest convertedRequest = B2Json.fromJsonOrThrowRuntime(json, B2SetBucketNotificationRulesRequest.class);

        // Compare json
        assertEquals(json, requestJson);

        // Compare requests
        assertEquals(b2SetBucketNotificationRulesRequest, convertedRequest);

    }
}