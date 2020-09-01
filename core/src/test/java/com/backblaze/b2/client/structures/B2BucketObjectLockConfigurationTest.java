package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import junit.framework.TestCase;
import org.junit.Test;

/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
public class B2BucketObjectLockConfigurationTest extends TestCase {

    @Test
    public void testDefaultConfig() {
        final String configJson = "{\n" +
                "   \"status\": \"disabled\"\n" +
                "}";
        final B2BucketObjectLockConfiguration convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2BucketObjectLockConfiguration.class);
        final B2BucketObjectLockConfiguration defaultConfig = new B2BucketObjectLockConfiguration(
                "disabled",null, null);
        assertEquals(defaultConfig, convertedConfig);
    }

    @Test
    public void testEquals() {
        final B2BucketObjectLockConfiguration config = new B2BucketObjectLockConfiguration(
                "enabled",
                21,
                "days",
                "governance");
        final String configJson = B2Json.toJsonOrThrowRuntime(config);
        final B2BucketObjectLockConfiguration convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2BucketObjectLockConfiguration.class);
        assertEquals(config, convertedConfig);
    }

    @Test
    public void testWithoutDisabledLockConfiguration() {
        final B2BucketObjectLockConfiguration config = new B2BucketObjectLockConfiguration(
                "disabled",
                null,
                null);
        final String configJson = B2Json.toJsonOrThrowRuntime(config);
        final B2BucketObjectLockConfiguration convertedConfig = B2Json.fromJsonOrThrowRuntime(
                configJson,
                B2BucketObjectLockConfiguration.class);
        assertEquals(config, convertedConfig);
    }

}