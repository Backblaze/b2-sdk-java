package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/*
 * Copyright 2020, Backblaze, Inc. All rights reserved.
 */
public class B2FileSseForResponseTest extends B2BaseTest {

    @Test
    public void testSseB2DefaultConfig() {
        final String jsonString = "{\n" +
                "  \"algorithm\": \"AES256\",\n" +
                "  \"mode\": \"SSE-B2\"\n" +
                "}";
        final B2FileSseForResponse converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileSseForResponse.class);
        final B2FileSseForResponse defaultConfig = new B2FileSseForResponse(
                B2ServerSideEncryptionMode.SSE_B2, "AES256", null);
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testSseCWithKeyMd5Config() {
        final String jsonString = "{\n" +
                "  \"algorithm\": \"AES256\",\n" +
                "  \"customerKeyMd5\": \"key MD5 string\",\n" +
                "  \"mode\": \"SSE-C\"\n" +
                "}";
        final B2FileSseForResponse converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileSseForResponse.class);
        final B2FileSseForResponse defaultConfig = new B2FileSseForResponse(
                B2ServerSideEncryptionMode.SSE_C, "AES256", "key MD5 string");
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testSseCWithoutKeyMd5Config() {
        final String jsonString = "{\n" +
                "  \"algorithm\": \"AES256\",\n" +
                "  \"mode\": \"SSE-C\"\n" +
                "}";
        final B2FileSseForResponse converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileSseForResponse.class);
        final B2FileSseForResponse defaultConfig = new B2FileSseForResponse(
                B2ServerSideEncryptionMode.SSE_C, "AES256", null);
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }
}