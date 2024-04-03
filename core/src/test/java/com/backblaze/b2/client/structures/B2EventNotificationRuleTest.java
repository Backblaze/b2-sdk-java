/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.TreeSet;

import static com.backblaze.b2.util.B2Collections.listOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class B2EventNotificationRuleTest extends B2BaseTest {

    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"eventTypes\": [\n" +
                "    \"b2:ObjectCreated:Replica\",\n" +
                "    \"b2:ObjectCreated:Upload\"\n" +
                "  ],\n" +
                "  \"isEnabled\": true,\n" +
                "  \"isSuspended\": false,\n" +
                "  \"name\": \"myRule\",\n" +
                "  \"objectNamePrefix\": \"\",\n" +
                "  \"suspensionReason\": \"\",\n" +
                "  \"targetConfiguration\": {\n" +
                "    \"customHeaders\": [\n" +
                "      {\n" +
                "        \"name\": \"name1\",\n" +
                "        \"value\": \"val1\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": \"name2\",\n" +
                "        \"value\": \"val2\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"hmacSha256SigningSecret\": \"3XDfkdQte2OgA78qCtSD17LAzpj6ay9H\",\n" +
                "    \"targetType\": \"webhook\",\n" +
                "    \"url\": \"https://www.example.com\"\n" +
                "  }\n" +
                "}";
        final B2EventNotificationRule converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2EventNotificationRule.class
                );

        final TreeSet<String> eventTypes = new TreeSet<>();
        eventTypes.add("b2:ObjectCreated:Replica");
        eventTypes.add("b2:ObjectCreated:Upload");
        final B2EventNotificationRule defaultConfig =
                new B2EventNotificationRule(
                        "myRule",
                        eventTypes,
                        "",
                        new B2WebhookConfiguration("" +
                                "https://www.example.com",
                                new TreeSet<>(
                                        listOf(
                                                new B2WebhookCustomHeader("name1", "val1"),
                                                new B2WebhookCustomHeader("name2", "val2")
                                        )
                                ),
                                "3XDfkdQte2OgA78qCtSD17LAzpj6ay9H"
                        ),
                        true,
                        false,
                        "");
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }
}
