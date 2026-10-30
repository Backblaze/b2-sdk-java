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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
                "    \"maxEventsPerBatch\": 20,\n" +
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
                                "3XDfkdQte2OgA78qCtSD17LAzpj6ay9H",
                                20
                        ),
                        true,
                        false,
                        "");
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testCompareToDistinguishesSigningSecrets() {
        final B2EventNotificationRule first = makeWebhookRule("signing-secret-one", "header-value", 20);
        final B2EventNotificationRule second = makeWebhookRule("signing-secret-two", "header-value", 20);

        assertFalse(first.equals(second));
        assertTrue(first.compareTo(second) != 0);

        final TreeSet<B2EventNotificationRule> rules = new TreeSet<>(listOf(first, second));
        assertEquals(2, rules.size());
    }

    @Test
    public void testCompareToDistinguishesCustomHeaderValues() {
        final B2EventNotificationRule first = makeWebhookRule("signing-secret", "header-value-one", 20);
        final B2EventNotificationRule second = makeWebhookRule("signing-secret", "header-value-two", 20);

        assertFalse(first.equals(second));
        assertTrue(first.compareTo(second) != 0);

        final TreeSet<B2EventNotificationRule> rules = new TreeSet<>(listOf(first, second));
        assertEquals(2, rules.size());
    }

    @Test
    public void testCompareToDistinguishesMaxEventsPerBatch() {
        final B2EventNotificationRule first = makeWebhookRule("signing-secret", "header-value", 1);
        final B2EventNotificationRule second = makeWebhookRule("signing-secret", "header-value", 2);

        assertFalse(first.equals(second));
        assertTrue(first.compareTo(second) != 0);
    }

    @Test
    public void testCompareToIsConsistentWithEquals() {
        final B2EventNotificationRule first = makeWebhookRule("signing-secret", "header-value", 20);
        final B2EventNotificationRule second = makeWebhookRule("signing-secret", "header-value", 20);

        assertEquals(first, second);
        assertEquals(0, first.compareTo(second));
    }

    @Test
    public void testCompareToOrdersUnknownTargetConfigurationsWithoutThrowing() {
        final B2EventNotificationRule first = new B2EventNotificationRule(
                "myRule",
                new TreeSet<>(listOf("b2:ObjectCreated:Upload")),
                "",
                new B2EventNotificationTargetConfiguration(),
                true,
                false,
                "");
        final B2EventNotificationRule second = new B2EventNotificationRule(
                "myRule",
                new TreeSet<>(listOf("b2:ObjectCreated:Upload")),
                "",
                new B2EventNotificationTargetConfiguration(),
                true,
                false,
                "");

        try {
            first.compareTo(second);
        } catch (IllegalArgumentException e) {
            fail("compareTo must support unknown target configurations: " + e.getMessage());
        }

        final TreeSet<B2EventNotificationRule> rules = new TreeSet<>();
        try {
            rules.add(first);
            rules.add(second);
        } catch (IllegalArgumentException e) {
            fail("TreeSet must support unknown target configurations: " + e.getMessage());
        }
        assertEquals(2, rules.size());
    }

    private B2EventNotificationRule makeWebhookRule(String signingSecret,
                                                     String customHeaderValue,
                                                     int maxEventsPerBatch) {
        return new B2EventNotificationRule(
                "myRule",
                new TreeSet<>(listOf("b2:ObjectCreated:Upload")),
                "",
                new B2WebhookConfiguration(
                        "https://www.example.com",
                        new TreeSet<>(listOf(new B2WebhookCustomHeader("X-Webhook-Authorization", customHeaderValue))),
                        signingSecret,
                        maxEventsPerBatch),
                true,
                false,
                "");
    }
}
