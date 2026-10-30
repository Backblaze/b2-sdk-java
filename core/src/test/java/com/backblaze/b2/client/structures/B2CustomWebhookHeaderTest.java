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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class B2CustomWebhookHeaderTest extends B2BaseTest {

    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"name\": \"name1\",\n" +
                "  \"value\": \"val1\"\n" +
                "}";
        final B2WebhookCustomHeader converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2WebhookCustomHeader.class,
                        B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS    // for targetType
                );
        final B2WebhookCustomHeader defaultConfig =
                new B2WebhookCustomHeader("name1", "val1");
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testToStringRedactsValue() {
        final B2WebhookCustomHeader header = new B2WebhookCustomHeader("X-Webhook-Authorization", "SECRET-WEBHOOK-HEADER");

        final String value = header.toString();

        assertFalse(value.contains("SECRET-WEBHOOK-HEADER"));
        assertTrue(value.contains("name='X-Webhook-Authorization'"));
        assertTrue(value.contains("value='[redacted]'"));
    }
}
