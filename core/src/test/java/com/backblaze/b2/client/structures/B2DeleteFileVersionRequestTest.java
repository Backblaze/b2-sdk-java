/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2DeleteFileVersionRequestTest extends B2BaseTest {

    @Test
    public void testNormal() {
        final B2DeleteFileVersionRequest request =
                B2DeleteFileVersionRequest.builder("fluffy.jpg", "4_zBlah_00000001").build();
        final String requestJson = B2Json.toJsonOrThrowRuntime(request);

        final String expectedJson = "{\n" +
                "  \"bypassGovernance\": false,\n" +
                "  \"fileId\": \"4_zBlah_00000001\",\n" +
                "  \"fileName\": \"fluffy.jpg\"\n" +
                "}";

        assertEquals(expectedJson, requestJson);
    }

    @Test
    public void testWithBypassGovernanceTrue() {
        final B2DeleteFileVersionRequest request = B2DeleteFileVersionRequest
                .builder("fluffy.jpg", "4_zBlah_00000001")
                .setBypassGovernance(true)
                .build();
        final String requestJson = B2Json.toJsonOrThrowRuntime(request);

        final String expectedJson = "{\n" +
                "  \"bypassGovernance\": true,\n" +
                "  \"fileId\": \"4_zBlah_00000001\",\n" +
                "  \"fileName\": \"fluffy.jpg\"\n" +
                "}";

        assertEquals(expectedJson, requestJson);
    }

    @Test
    public void testWithBypassGovernanceFalse() {
        final B2DeleteFileVersionRequest request = B2DeleteFileVersionRequest
                .builder("fluffy.jpg", "4_zBlah_00000001")
                .setBypassGovernance(false)
                .build();
        final String requestJson = B2Json.toJsonOrThrowRuntime(request);

        final String expectedJson = "{\n" +
                "  \"bypassGovernance\": false,\n" +
                "  \"fileId\": \"4_zBlah_00000001\",\n" +
                "  \"fileName\": \"fluffy.jpg\"\n" +
                "}";

        assertEquals(expectedJson, requestJson);
    }
}