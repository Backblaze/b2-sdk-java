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

import static com.backblaze.b2.util.B2Collections.listOf;
import static org.junit.Assert.assertEquals;

public class B2EventNotificationRuleForRequestTest extends B2BaseTest {
    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"eventTypes\": [\n" +
                "    \"b2:ObjectCreated:Replica\",\n" +
                "    \"b2:ObjectCreated:Upload\"\n" +
                "  ],\n" +
                "  \"isEnabled\": true,\n" +
                "  \"name\": \"myRule\",\n" +
                "  \"objectNamePrefix\": \"\",\n" +
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
                "    \"targetType\": \"webhook\",\n" +
                "    \"url\": \"https://www.example.com\"\n" +
                "  }\n" +
                "}";
        final B2EventNotificationRuleForRequest converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2EventNotificationRuleForRequest.class
                );

        final TreeSet<String> eventTypes = new TreeSet<>();
        eventTypes.add("b2:ObjectCreated:Replica");
        eventTypes.add("b2:ObjectCreated:Upload");
        final B2EventNotificationRuleForRequest defaultConfig =
                new B2EventNotificationRuleForRequest(
                        "myRule",
                        eventTypes,
                        "",
                        new B2WebhookConfigurationForRequest("" +
                                "https://www.example.com",
                                new TreeSet<>(
                                        listOf(
                                                new B2CustomHeaderForRequest("name1", "val1"),
                                                new B2CustomHeaderForRequest("name2", "val2")
                                        )
                                )
                        ),
                        true
                );
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testConvertToListOfB2EventNotificationRuleForResponse() {
        final List<B2EventNotificationRuleForResponse> original = listOf(
                new B2EventNotificationRuleForResponse(
                        "myRuleName",
                        new TreeSet<>(listOf("b2:ObjectCreated:Replica", "b2:ObjectCreated:Upload")),
                        "",
                        new B2WebhookConfigurationForResponse(
                                "https://www.example.com",
                                new TreeSet<>(
                                        listOf(
                                                new B2CustomHeaderForResponse("name1", "val1"),
                                                new B2CustomHeaderForResponse("name2", "val2")
                                        )
                                ),
                                "dummyHmacSha256SigningSecret"
                        ),
                        true
                ),
                new B2EventNotificationRuleForResponse(
                        "myRuleName2",
                        new TreeSet<>(listOf("b2:ObjectCreated:Copy")),
                        "myObjectNamePrefix",
                        new B2WebhookConfigurationForResponse(
                                "https://www.example2.com",
                                "dummyHmacSha256SigningSecret2"
                        ),
                        false
                )
        );

        final List<B2EventNotificationRuleForRequest> convertedList =
            B2EventNotificationRuleForRequest.convertToListOfB2EventNotificationRuleForRequest(original);

        final List<B2EventNotificationRuleForRequest> expectedList = listOf(
                new B2EventNotificationRuleForRequest(
                        "myRuleName",
                        new TreeSet<>(listOf("b2:ObjectCreated:Replica", "b2:ObjectCreated:Upload")),
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
                ),
                new B2EventNotificationRuleForRequest(
                        "myRuleName2",
                        new TreeSet<>(listOf("b2:ObjectCreated:Copy")),
                        "myObjectNamePrefix",
                        new B2WebhookConfigurationForRequest(
                                "https://www.example2.com"
                        ),
                        false
                )
        );

        assertEquals(expectedList, convertedList);
    }
}