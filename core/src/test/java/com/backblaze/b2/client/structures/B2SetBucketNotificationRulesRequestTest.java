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

        final List<B2EventNotificationRule> eventNotificationRuleList = listOf(
                new B2EventNotificationRule(
                        "ruleName",
                        new TreeSet<>(listOf("b2:ObjectCreated:Copy")),
                        "",
                        new B2WebhookConfiguration("https://www.example.com"),
                        true
                ),
                new B2EventNotificationRule(
                        "ruleNameWithCustomHeaders",
                        new TreeSet<>(listOf("b2:ObjectCreated:Replica")),
                        "",
                        new B2WebhookConfiguration(
                                "https://www.example.com",
                                new TreeSet<>(
                                        listOf(
                                                new B2WebhookCustomHeader("name1", "val1"),
                                                new B2WebhookCustomHeader("name2", "val2")
                                        )
                                ),
                                "rrzaVL6BqYt83s2Q5R2I79AilaxVBJUS",
                                20
                        ),
                        true
                )
        );

        final B2SetBucketNotificationRulesRequest b2SetBucketNotificationRulesRequest =
                B2SetBucketNotificationRulesRequest.builder(BUCKET_ID, eventNotificationRuleList)
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
                "      \"isSuspended\": null,\n" +
                "      \"name\": \"ruleName\",\n" +
                "      \"objectNamePrefix\": \"\",\n" +
                "      \"suspensionReason\": null,\n" +
                "      \"targetConfiguration\": {\n" +
                "        \"customHeaders\": null,\n" +
                "        \"hmacSha256SigningSecret\": null,\n" +
                "        \"targetType\": \"webhook\",\n" +
                "        \"url\": \"https://www.example.com\"\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"eventTypes\": [\n" +
                "        \"b2:ObjectCreated:Replica\"\n" +
                "      ],\n" +
                "      \"isEnabled\": true,\n" +
                "      \"isSuspended\": null,\n" +
                "      \"name\": \"ruleNameWithCustomHeaders\",\n" +
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
                "        \"hmacSha256SigningSecret\": \"rrzaVL6BqYt83s2Q5R2I79AilaxVBJUS\",\n" +
                "        \"maxEventsPerBatch\": 20,\n" +
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
