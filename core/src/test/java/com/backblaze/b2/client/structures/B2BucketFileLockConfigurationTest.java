package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import junit.framework.TestCase;
import org.junit.Test;

/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
public class B2BucketFileLockConfigurationTest extends TestCase {

    @Test
    public void testDefaultConfig() {
        final String configJson = "{\n" +
                "   \"status\": \"disabled\"\n" +
                "}";
        final B2BucketFileLockConfiguration convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2BucketFileLockConfiguration.class);
        final B2BucketFileLockConfiguration defaultConfig = new B2BucketFileLockConfiguration(
                "disabled",null, null);
        assertEquals(defaultConfig, convertedConfig);
    }

    @Test
    public void testEquals() {
        final B2BucketFileLockConfiguration config = new B2BucketFileLockConfiguration(
                "enabled",
                21,
                "days",
                "governance");
        final String configJson = B2Json.toJsonOrThrowRuntime(config);
        final B2BucketFileLockConfiguration convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2BucketFileLockConfiguration.class);
        assertEquals(config, convertedConfig);
    }

    @Test
    public void testWithoutDisabledLockConfiguration() {
        final B2BucketFileLockConfiguration config = new B2BucketFileLockConfiguration(
                "disabled",
                null,
                null);
        final String configJson = B2Json.toJsonOrThrowRuntime(config);
        final B2BucketFileLockConfiguration convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2BucketFileLockConfiguration.class);
        assertEquals(config, convertedConfig);
    }

}