/*
 * Copyright 2021, Backblaze, Inc.  All rights reserved.
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2BucketDefaultRetentionTest extends B2BaseTest {

    @Test
    public void testDefaultConfigJson() {
        final String configJson = "{\n" +
                "  \"mode\": null,\n" +
                "  \"period\": null\n" +
                "}";
        final B2BucketDefaultRetention convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2BucketDefaultRetention.class);
        final B2BucketDefaultRetention defaultConfig = new B2BucketDefaultRetention(null, null);
        assertEquals(defaultConfig, convertedConfig);
    }

    @Test
    public void testFullConfigJson() {
        final String configJson = "{\n" +
                "  \"mode\": \"governance\",\n" +
                "  \"period\": {\n" +
                "    \"duration\": 10,\n" +
                "    \"unit\": \"years\"\n" +
                "  }\n" +
                "}";
        final B2BucketDefaultRetention convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2BucketDefaultRetention.class);
        final B2BucketDefaultRetention defaultConfig = new B2BucketDefaultRetention(
                B2FileRetentionMode.GOVERNANCE,
                10,
                B2FileRetentionPeriodUnit.YEARS);
        assertEquals(defaultConfig, convertedConfig);
    }

}