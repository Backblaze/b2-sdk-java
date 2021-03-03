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
public class B2FileRetentionTest extends B2BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testNullMode() {
        thrown.expect(IllegalArgumentException.class);
        new B2FileRetention(null, 123456789L);
    }

    @Test
    public void testNullRetainUntilTimestamp() {
        thrown.expect(IllegalArgumentException.class);
        new B2FileRetention("compliance", null);
    }

    @Test
    public void testGetFileLockFromHeadersOrNull() {
        final B2Headers b2Headers = B2HeadersImpl.builder()
                .set("X-Bz-File-Retention-Mode", "compliance")
                .set("X-Bz-File-Retention-Retain-Until-Timestamp", "123456789")
                .build();
        final B2FileRetention b2FileRetention = B2FileRetention.getFileRetentionFromHeadersOrNull(b2Headers);
        assertEquals(new B2FileRetention("compliance", 123456789L), b2FileRetention);

        final B2Headers b2HeadersNull = null;
        //noinspection ConstantConditions
        final B2FileRetention b2FileRetentionNull = B2FileRetention.getFileRetentionFromHeadersOrNull(b2HeadersNull);
        assertNull(b2FileRetentionNull);
    }

    @Test
    public void testEquals() {
        final B2FileRetention config = new B2FileRetention(
                "governance",
                123456L);
        final String jsonString = B2Json.toJsonOrThrowRuntime(config);
        final B2FileRetention convertedConfig = B2Json.fromJsonOrThrowRuntime(
                jsonString,
                B2FileRetention.class);
        assertEquals(config, convertedConfig);
    }
}