/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2ReplicationRuleTest extends B2BaseTest {
    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"destinationBucketId\": \"000011112222333344445555\",\n" +
                "  \"fileNamePrefix\": \"\",\n" +
                "  \"includeExistingFiles\": true,\n" +
                "  \"isEnabled\": false,\n" +
                "  \"priority\": 3,\n" +
                "  \"replicationRuleName\": \"my-replication-rule\"\n" +
                "}";
        final B2ReplicationRule converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2ReplicationRule.class
                );
        final B2ReplicationRule defaultConfig =
                new B2ReplicationRule(
                        "my-replication-rule",
                        "000011112222333344445555",
                        3,
                        "",
                        false,
                        true
                );
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }
}
