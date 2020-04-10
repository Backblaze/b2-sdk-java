/*
 * Copyright 2019, Backblaze, Inc.  All rights reserved.
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.B2TestHelpers;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class B2ApplicationKeyTest extends B2BaseTest {

    @Test
    public void testEquals() {
        final TreeSet<String> capabilities = new TreeSet<>();
        capabilities.add(B2Capabilities.WRITE_FILES);
        capabilities.add(B2Capabilities.READ_FILES);
        capabilities.add(B2Capabilities.LIST_ALL_BUCKET_NAMES);
        capabilities.add(B2Capabilities.READ_BUCKETS);
        final B2ApplicationKey applicationKey =
                new B2ApplicationKey(
                        "accountId",
                        "appKeyId",
                        "keyName",
                        capabilities,
                        "bucketId",
                        "namePrefix",
                        12345678L,
                        B2TestHelpers.makeBucketOrApplicationKeyOptions()
                );
        final String applicationKeyJson = B2Json.toJsonOrThrowRuntime(applicationKey);
        final B2ApplicationKey convertedApplicationKey =
                B2Json.fromJsonOrThrowRuntime(applicationKeyJson, B2ApplicationKey.class);
        assertEquals(applicationKey, convertedApplicationKey);
    }
}
