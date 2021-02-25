package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.contentSources.B2HeadersImpl;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

/*
 * Copyright 2020, Backblaze, Inc. All rights reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
public class B2FileLockTest extends B2BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testNullMode() {
        thrown.expect(IllegalArgumentException.class);
        new B2FileLock(null, 123456789L);
    }

    @Test
    public void testNullRetainUntilTimestamp() {
        thrown.expect(IllegalArgumentException.class);
        new B2FileLock("compliance", null);
    }

    @Test
    public void testGetFileLockFromHeadersOrNull() {
        final B2Headers b2Headers = B2HeadersImpl.builder()
                .set("X-Bz-File-Lock-Retention-Mode", "compliance")
                .set("X-Bz-File-Lock-Retention-Retain-Until-Timestamp", "123456789")
                .build();
        final B2FileLock b2FileLock = B2FileLock.getFileLockFromHeadersOrNull(b2Headers);
        assertEquals(new B2FileLock("compliance", 123456789L), b2FileLock);

        final B2Headers b2HeadersNull = null;
        final B2FileLock b2FileLockNull = B2FileLock.getFileLockFromHeadersOrNull(b2HeadersNull);
        assertNull(b2FileLockNull);
    }

    @Test
    public void testEquals() {
        final B2FileLock config = new B2FileLock(
                "governance",
                123456L);
        final String jsonString = B2Json.toJsonOrThrowRuntime(config);
        final B2FileLock convertedConfig = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileLock.class);
        assertEquals(config, convertedConfig);
    }
}