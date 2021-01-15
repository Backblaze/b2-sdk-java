package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Base64;

import static org.junit.Assert.assertEquals;

/*
 * Copyright 2020, Backblaze, Inc. All rights reserved.
 */
public class B2FileSseForRequestTest extends B2BaseTest {

    @Test
    public void testDefaultBackblazeManagedKeyConfig() {
        final String jsonString = "{\n" +
                "  \"algorithm\": \"AES256\",\n" +
                "  \"mode\": \"SSE-B2\"\n" +
                "}";
        final B2FileSseForRequest converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileSseForRequest.class);
        final B2FileSseForRequest defaultConfig = B2FileSseForRequest.createSseB2Aes256();
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testFullSseCustomerManagedKeyConfig() {
        final String key = "iLNDwUxG7jW5Dk8K4L5MmtRlFYGtHCPWWYkzpFZ6cb8=";
        final String keyMd5 = "uNesypp/GNphraVA9wPL5A==";

        final String jsonString = "{\n" +
                "  \"algorithm\": \"AES256\",\n" +
                "  \"customerKey\": \"" + key + "\",\n" +
                "  \"customerKeyMd5\": \"" + keyMd5 + "\",\n" +
                "  \"mode\": \"SSE-C\"\n" +
                "}";
        final B2FileSseForRequest converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileSseForRequest.class);
        final B2FileSseForRequest defaultConfig = B2FileSseForRequest.createSseCAes256(key, keyMd5);
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testSseCustomerManagedKeyWithCalculatedMd5Config() {
        final String key = "iLNDwUxG7jW5Dk8K4L5MmtRlFYGtHCPWWYkzpFZ6cb8=";
        final String expectedKeyMd5 = "uNesypp/GNphraVA9wPL5A==";

        final String jsonString = "{\n" +
                "  \"algorithm\": \"AES256\",\n" +
                "  \"customerKey\": \"" + key + "\",\n" +
                "  \"customerKeyMd5\": \"" + expectedKeyMd5 + "\",\n" +
                "  \"mode\": \"SSE-C\"\n" +
                "}";
        final B2FileSseForRequest converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileSseForRequest.class);
        final B2FileSseForRequest defaultConfig = B2FileSseForRequest.createSseCAes256(key);
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testMissingMode() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert from json: required field mode is missing");

        final String jsonString = "{\n" +
                "   \"algorithm\": \"AES256\"\n" +
                "}";
        B2Json.fromJsonOrThrowRuntime(jsonString, B2FileSseForRequest.class);
    }

    @Test
    public void testMissingAlgorithm() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert from json: required field algorithm is missing");

        final String jsonString = "{\n" +
                "   \"mode\": \"SSE-B2\"\n" +
                "}";
        B2Json.fromJsonOrThrowRuntime(jsonString, B2FileSseForRequest.class);
    }

    @Test
    public void testMissingBoth() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert from json: required field algorithm is missing");

        final String jsonString = "{\n" +
                "}";
        B2Json.fromJsonOrThrowRuntime(jsonString, B2FileSseForRequest.class);
    }

    @Test
    public void testWithNullAlgorithm() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert from json: required field algorithm cannot be null");
        final String jsonString = "{\n" +
                "   \"algorithm\": null,\n" +
                "   \"mode\": \"SSE-B2\"\n" +
                "}";
        B2Json.fromJsonOrThrowRuntime(jsonString, B2FileSseForRequest.class);
    }


    @Test
    public void testWithNullMode() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert from json: required field mode cannot be null");
        final String jsonString = "{\n" +
                "   \"algorithm\": \"AES256\",\n" +
                "   \"mode\": null\n" +
                "}";
        B2Json.fromJsonOrThrowRuntime(jsonString, B2FileSseForRequest.class);
    }

    @Test
    public void testNull() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert from json: required field algorithm cannot be null");
        final String jsonString = "{\n" +
                "   \"algorithm\": null,\n" +
                "   \"mode\": null\n" +
                "}";
        B2Json.fromJsonOrThrowRuntime(jsonString, B2FileSseForRequest.class);
    }
}