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
public class B2BucketServerSideEncryptionTest extends B2BaseTest {
    @Test
    public void testDefaultConfig() {
        final String jsonString = "{\n" +
                "  \"algorithm\": \"AES256\",\n" +
                "  \"mode\": \"SSE-B2\"\n" +
                "}";
        final B2BucketServerSideEncryption converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2BucketServerSideEncryption.class);
        final B2BucketServerSideEncryption defaultConfig = new B2BucketServerSideEncryption(
                B2ServerSideEncryptionMode.SSE_B2, "AES256");
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
        B2Json.fromJsonOrThrowRuntime(jsonString, B2BucketServerSideEncryption.class);
    }

    @Test
    public void testMissingBoth() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert from json: required field mode is missing");

        final String jsonString = "{\n" +
                "}";
        B2Json.fromJsonOrThrowRuntime(jsonString, B2BucketServerSideEncryption.class);
    }

    @Test
    public void testWithNullMode() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert from json: required field mode cannot be null");
        final String jsonString = "{\n" +
                "   \"algorithm\": \"AES256\",\n" +
                "   \"mode\": null\n" +
                "}";
        B2Json.fromJsonOrThrowRuntime(jsonString, B2BucketServerSideEncryption.class);
    }

    @Test
    public void testNull() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert from json: required field mode cannot be null");
        final String jsonString = "{\n" +
                "   \"algorithm\": null,\n" +
                "   \"mode\": null\n" +
                "}";
        B2Json.fromJsonOrThrowRuntime(jsonString, B2BucketServerSideEncryption.class);
    }
}