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
        final B2BucketServerSideEncryption defaultConfig = B2BucketServerSideEncryption.createSseB2Aes256();
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testSseNoneConfig() {
        final String jsonString = "{\n" +
                "  \"algorithm\": null,\n" +
                "  \"mode\": \"none\"\n" +
                "}";
        final B2BucketServerSideEncryption converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2BucketServerSideEncryption.class);
        final B2BucketServerSideEncryption defaultConfig = B2BucketServerSideEncryption.createSseNone();
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }
}