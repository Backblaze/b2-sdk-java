package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import junit.framework.TestCase;
import org.junit.Test;

/*
 * Copyright 2020, Backblaze, Inc. All rights reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
public class B2FileLockTest extends TestCase {
    @Test
    public void testDefaultConfig() {
        final String jsonString = "{\n" +
                "   \"status\": \"off\"\n" +
                "}";
        final B2FileLock converted = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileLock.class);
        final B2FileLock defaultConfig = new B2FileLock(
                "off", null, null);
        assertEquals(defaultConfig, converted);
    }

    @Test
    public void testEquals() {
        final B2FileLock config = new B2FileLock(
                "on",
                "governance",
                123456L);
        final String jsonString = B2Json.toJsonOrThrowRuntime(config);
        final B2FileLock convertedConfig = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileLock.class);
        assertEquals(config, convertedConfig);
    }

    @Test
    public void testWithoutDisabledLockStatus() {
        final B2FileLock config = new B2FileLock(
                "off",
                null,
                null);
        final String jsonString = B2Json.toJsonOrThrowRuntime(config);
        final B2FileLock convertedConfig = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileLock.class);
        assertEquals(config, convertedConfig);
    }

    @Test
    public void testWithoutUnauthorizedLockStatus() {
        final B2FileLock config = new B2FileLock(
                "unauthorized",
                null,
                null);
        final String jsonString = B2Json.toJsonOrThrowRuntime(config);
        final B2FileLock convertedConfig = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileLock.class);
        assertEquals(config, convertedConfig);
    }
}