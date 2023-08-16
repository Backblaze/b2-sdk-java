/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.json.B2JsonOptions;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class B2WebhookConfigurationTest extends B2BaseTest {

    @Test
    public void testUrlWithIncorrectProtocolThrows() {
        // Must be https://
        try {
            new B2WebhookConfiguration("http://www.backblaze.com");
            fail("should have thrown");
        }
        catch (IllegalArgumentException e) {
            assertEquals("The protocol for the url must be https://", e.getMessage());
        }
    }

    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"targetType\": \"webhook\",\n" +
                "  \"url\": \"https://www.example.com\"\n" +
                "}";
        final B2WebhookConfiguration converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2WebhookConfiguration.class,
                        B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS    // for targetType
                );
        final B2WebhookConfiguration defaultConfig =
                new B2WebhookConfiguration(
                        "https://www.example.com"
                );
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }
}
