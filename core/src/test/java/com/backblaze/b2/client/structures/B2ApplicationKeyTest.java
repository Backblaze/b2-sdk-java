/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
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
        capabilities.add(B2Capabilities.BYPASS_GOVERNANCE);
        capabilities.add(B2Capabilities.READ_BUCKET_RETENTIONS);
        capabilities.add(B2Capabilities.WRITE_BUCKET_RETENTIONS);
        capabilities.add(B2Capabilities.READ_FILE_RETENTIONS);
        capabilities.add(B2Capabilities.WRITE_FILE_RETENTIONS);
        capabilities.add(B2Capabilities.READ_FILE_LEGAL_HOLDS);
        capabilities.add(B2Capabilities.WRITE_FILE_LEGAL_HOLDS);
        capabilities.add(B2Capabilities.READ_BUCKET_REPLICATIONS);
        capabilities.add(B2Capabilities.WRITE_BUCKET_REPLICATIONS);
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
