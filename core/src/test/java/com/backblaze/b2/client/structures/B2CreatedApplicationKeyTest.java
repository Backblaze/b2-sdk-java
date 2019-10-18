/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.B2TestHelpers;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class B2CreatedApplicationKeyTest extends B2BaseTest {

    @Test
    public void testToApplicationKey() {
        final TreeSet<String> capabilities = new TreeSet<>();
        capabilities.add(B2Capabilities.WRITE_FILES);

        assertEquals(
                new B2ApplicationKey(
                        "accountId",
                        "appKeyId",
                        "keyName",
                        capabilities,
                        "bucketId",
                        "namePrefix",
                        12345678L,
                        B2TestHelpers.makeBucketOrApplicationKeyOptions()
                ),
                new B2CreatedApplicationKey(

                        "accountId",
                        "appKeyId",
                        "appKeySecret",
                        "keyName",
                        capabilities,
                        "bucketId",
                        "namePrefix",
                        12345678L,
                        B2TestHelpers.makeBucketOrApplicationKeyOptions()
                ).toApplicationKey()
        );
    }

    @Test
    public void testEquals() {
        final TreeSet<String> capabilities = new TreeSet<>();
        capabilities.add(B2Capabilities.WRITE_FILES);
        capabilities.add(B2Capabilities.READ_FILES);
        final B2CreatedApplicationKey createdApplicationKey =
                new B2CreatedApplicationKey(
                        "accountId",
                        "appKeyId",
                        "appKey",
                        "keyName",
                        capabilities,
                        "bucketId",
                        "namePrefix",
                        12345678L,
                        B2TestHelpers.makeBucketOrApplicationKeyOptions()
                );
        final String createdApplicationKeyJson = B2Json.toJsonOrThrowRuntime(createdApplicationKey);
        final B2CreatedApplicationKey convertedCreatedApplicationKey =
                B2Json.fromJsonOrThrowRuntime(
                        createdApplicationKeyJson,
                        B2CreatedApplicationKey.class);
        assertEquals(createdApplicationKey, convertedCreatedApplicationKey);
    }
}
