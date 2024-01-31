/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.json.B2JsonOptions;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.TreeSet;

import static com.backblaze.b2.util.B2Collections.listOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class B2WebhookConfigurationForResponseTest extends B2BaseTest {

    @Test
    public void testUrlWithIncorrectProtocolThrows() {
        // Must be https://
        try {
            //noinspection HttpUrlsUsage
            new B2WebhookConfigurationForResponse(
                    "http://www.example.com",
                    new TreeSet<>(
                            listOf(
                                    new B2CustomHeaderForResponse("name1", "val1"),
                                    new B2CustomHeaderForResponse("name2", "val2")
                            )
                    ),
                    "dummySigningSecret"
            );
            fail("should have thrown");
        }
        catch (IllegalArgumentException e) {
            assertEquals("The protocol for the url must be https://", e.getMessage());
        }
    }

    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"customHeaders\": [\n" +
                "    {\n" +
                "      \"name\": \"name1\",\n" +
                "      \"value\": \"val1\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"name2\",\n" +
                "      \"value\": \"val2\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"hmacSha256SigningSecret\": \"dummySigningSecret\",\n" +
                "  \"targetType\": \"webhook\",\n" +
                "  \"url\": \"https://www.example.com\"\n" +
                "}";
        final B2WebhookConfigurationForResponse converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2WebhookConfigurationForResponse.class,
                        B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS    // for targetType
                );
        final B2WebhookConfigurationForResponse defaultConfig =
                new B2WebhookConfigurationForResponse(
                        "https://www.example.com",
                        new TreeSet<>(
                                listOf(
                                        new B2CustomHeaderForResponse("name1", "val1"),
                                        new B2CustomHeaderForResponse("name2", "val2")
                                )
                        ),
                        "dummySigningSecret"
                );
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }
}