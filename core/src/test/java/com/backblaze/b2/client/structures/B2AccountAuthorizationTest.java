/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class B2AccountAuthorizationTest {

    @Test
    public void testToStringRedactsAuthorizationToken() {
        final B2AccountAuthorization authorization = new B2AccountAuthorization(
                "account-id",
                "SECRET-ACCOUNT-TOKEN",
                "https://api.example.com",
                "https://download.example.com",
                100,
                50,
                new B2Allowed(B2Collections.listOf(B2Capabilities.LIST_FILES), "bucket-id", "bucket-name", "prefix"),
                "https://s3.example.com");

        final String value = authorization.toString();

        assertFalse(value.contains("SECRET-ACCOUNT-TOKEN"));
        assertTrue(value.contains("authorizationToken='[redacted]'"));
        assertTrue(value.contains("accountId='account-id'"));
        assertTrue(value.contains("apiUrl='https://api.example.com'"));
        assertTrue(value.contains("downloadUrl='https://download.example.com'"));
        assertTrue(value.contains("recommendedPartSize=100"));
        assertTrue(value.contains("absoluteMinimumPartSize=50"));
        assertTrue(value.contains("allowed="));
        assertTrue(value.contains("s3ApiUrl=https://s3.example.com"));
    }

    @Test
    public void testJsonRoundTripKeepsAuthorizationToken() {
        final B2AccountAuthorization authorization = new B2AccountAuthorization(
                "account-id",
                "SECRET-ACCOUNT-TOKEN",
                "https://api.example.com",
                "https://download.example.com",
                100,
                50,
                new B2Allowed(B2Collections.listOf(B2Capabilities.LIST_FILES), "bucket-id", "bucket-name", "prefix"),
                "https://s3.example.com");

        final String json = B2Json.toJsonOrThrowRuntime(authorization);

        assertTrue(json.contains("\"authorizationToken\": \"SECRET-ACCOUNT-TOKEN\""));
        assertEquals(authorization, B2Json.fromJsonOrThrowRuntime(json, B2AccountAuthorization.class));
    }

    @Test
    public void testToStringShowsNullAuthorizationToken() {
        final B2AccountAuthorization authorization = new B2AccountAuthorization(
                "account-id",
                null,
                "https://api.example.com",
                "https://download.example.com",
                100,
                50,
                new B2Allowed(B2Collections.listOf(B2Capabilities.LIST_FILES), "bucket-id", "bucket-name", "prefix"),
                "https://s3.example.com");

        final String value = authorization.toString();

        assertTrue(value.contains("authorizationToken='null'"));
        assertFalse(value.contains("authorizationToken='[redacted]'"));
    }
}
