/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class B2BucketFileLockConfigurationTest {

    @Test
    public void testDefaultConfig() {
        final String configJson = "{\n" +
                "  \"defaultRetention\": {\n" +
                "    \"mode\": null,\n" +
                "    \"period\": null\n" +
                "  },\n" +
                "  \"isFileLockEnabled\": false\n" +
                "}";
        final B2BucketFileLockConfiguration convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2BucketFileLockConfiguration.class);
        final B2BucketFileLockConfiguration defaultConfig = new B2BucketFileLockConfiguration(false);
        Assert.assertEquals(defaultConfig, convertedConfig);
    }

    @Test
    public void testEquals() {
        final B2BucketFileLockConfiguration config = new B2BucketFileLockConfiguration(
                false,
                B2FileRetentionMode.GOVERNANCE,
                21,
                B2FileRetentionPeriodUnit.DAYS
        );
        final String configJson = B2Json.toJsonOrThrowRuntime(config);
        final B2BucketFileLockConfiguration convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2BucketFileLockConfiguration.class);
        Assert.assertEquals(config, convertedConfig);
    }

    @Test
    public void testWithoutDisabledLockConfiguration() {
        final B2BucketFileLockConfiguration config = new B2BucketFileLockConfiguration(false);
        final String configJson = B2Json.toJsonOrThrowRuntime(config);
        final B2BucketFileLockConfiguration convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2BucketFileLockConfiguration.class);
        Assert.assertEquals(config, convertedConfig);
    }

}