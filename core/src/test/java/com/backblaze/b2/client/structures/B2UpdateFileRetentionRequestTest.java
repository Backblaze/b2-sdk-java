/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class B2UpdateFileRetentionRequestTest extends B2BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testNormal() {
        final B2UpdateFileRetentionRequest request = B2UpdateFileRetentionRequest
                .builder("fluffy.jpg", "file-id", new B2FileRetention(
                        B2FileRetentionMode.GOVERNANCE, 1000L))
                .build();
        final String requestJson = B2Json.toJsonOrThrowRuntime(request);

        final String expectedJson = "{\n" +
                "  \"bypassGovernance\": false,\n" +
                "  \"fileId\": \"file-id\",\n" +
                "  \"fileName\": \"fluffy.jpg\",\n" +
                "  \"fileRetention\": {\n" +
                "    \"mode\": \"governance\",\n" +
                "    \"retainUntilTimestamp\": 1000\n" +
                "  }\n" +
                "}";

        assertEquals(expectedJson, requestJson);
    }

    @Test
    public void testNormalWithBypassGovernance() {
        final B2UpdateFileRetentionRequest request = B2UpdateFileRetentionRequest
                .builder("fluffy.jpg", "file-id", new B2FileRetention(
                        B2FileRetentionMode.GOVERNANCE, 1000L))
                .setBypassGovernance(true)
                .build();
        final String requestJson = B2Json.toJsonOrThrowRuntime(request);

        final String expectedJson = "{\n" +
                "  \"bypassGovernance\": true,\n" +
                "  \"fileId\": \"file-id\",\n" +
                "  \"fileName\": \"fluffy.jpg\",\n" +
                "  \"fileRetention\": {\n" +
                "    \"mode\": \"governance\",\n" +
                "    \"retainUntilTimestamp\": 1000\n" +
                "  }\n" +
                "}";

        assertEquals(expectedJson, requestJson);
    }

    @Test
    public void testWithNullModeAndRetainUntilTimestamp() {
        final B2UpdateFileRetentionRequest request = B2UpdateFileRetentionRequest
                .builder("fluffy.jpg", "file-id", B2FileRetention.NONE)
                .setBypassGovernance(true)
                .build();
        final String requestJson = B2Json.toJsonOrThrowRuntime(request);

        final String expectedJson = "{\n" +
                "  \"bypassGovernance\": true,\n" +
                "  \"fileId\": \"file-id\",\n" +
                "  \"fileName\": \"fluffy.jpg\",\n" +
                "  \"fileRetention\": {\n" +
                "    \"mode\": null,\n" +
                "    \"retainUntilTimestamp\": null\n" +
                "  }\n" +
                "}";

        assertEquals(expectedJson, requestJson);
    }

    @Test
    public void testInvalidModeThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid value for file retention mode");

        final B2UpdateFileRetentionRequest ignored = B2UpdateFileRetentionRequest
                .builder("fluffy.jpg", "file-id", new B2FileRetention("keeper", 1000L))
                .build();
    }

    @Test
    public void testNullModeWithValidRetainUntilTimestampThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Both file retention mode and retainUntilTimestamp are required if either is supplied");

        final B2UpdateFileRetentionRequest ignored = B2UpdateFileRetentionRequest
                .builder("fluffy.jpg", "file-id", new B2FileRetention(null, 1000L))
                .build();
    }

    @Test
    public void testValidModeWithNullRetainUntilTimestampThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Both file retention mode and retainUntilTimestamp are required if either is supplied");

        final B2UpdateFileRetentionRequest ignored = B2UpdateFileRetentionRequest
                .builder("fluffy.jpg", "file-id", new B2FileRetention(B2FileRetentionMode.GOVERNANCE, null))
                .build();
    }

}