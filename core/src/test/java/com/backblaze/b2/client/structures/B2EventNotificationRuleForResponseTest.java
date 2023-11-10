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

public class B2EventNotificationRuleForResponseTest extends B2BaseTest {

    @Test
    public void testIsSuspendedWithoutSuspensionReasonThrows() {
        try {
            new B2EventNotificationRuleForResponse(
                    "myRule",
                    new TreeSet<>(
                            listOf("b2:ObjectCreated:Replica", "b2:ObjectCreated:Upload")
                    ),
                    "",
                    new B2WebhookConfigurationForResponse("" +
                            "https://www.example.com",
                            new TreeSet<>(
                                    listOf(
                                            new B2CustomHeaderForResponse("name1", "val1"),
                                            new B2CustomHeaderForResponse("name2", "val2")
                                    )
                            ),
                            "dummySigningSecret"
                    ),
                    true,
                    true,
                    ""
            );
            fail("should have thrown");
        }
        catch (IllegalArgumentException e) {
            assertEquals("A suspension reason is required if isSuspended is true", e.getMessage());
        }
    }

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
                "    \"hmacSha256SigningSecret\": \"dummySigningSecret\",\n" +
                "    \"targetType\": \"webhook\",\n" +
                "    \"url\": \"https://www.example.com\"\n" +
                "  }\n" +
                "}";
        final B2EventNotificationRuleForResponse converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2EventNotificationRuleForResponse.class
                );

        final TreeSet<String> eventTypes = new TreeSet<>();
        eventTypes.add("b2:ObjectCreated:Replica");
        eventTypes.add("b2:ObjectCreated:Upload");
        final B2EventNotificationRuleForResponse defaultConfig =
                new B2EventNotificationRuleForResponse(
                        "myRule",
                        eventTypes,
                        "",
                        new B2WebhookConfigurationForResponse("" +
                                "https://www.example.com",
                                new TreeSet<>(
                                        listOf(
                                                new B2CustomHeaderForResponse("name1", "val1"),
                                                new B2CustomHeaderForResponse("name2", "val2")
                                        )
                                ),
                                "dummySigningSecret"
                        ),
                        true,
                        false,
                        "");
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }
}
