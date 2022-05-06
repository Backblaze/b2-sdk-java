/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.backblaze.b2.util.B2Collections.listOf;
import static com.backblaze.b2.util.B2Collections.mapOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class B2BucketReplicationConfigurationTest extends B2BaseTest {

    @Test
    public void testToJsonAndBack_sourceOnly() {
        final String jsonString = "{\n" +
                "  \"asReplicationDestination\": null,\n" +
                "  \"asReplicationSource\": {\n" +
                "    \"replicationRules\": [\n" +
                "      {\n" +
                "        \"destinationBucketId\": \"000011112222333344445555\",\n" +
                "        \"fileNamePrefix\": \"\",\n" +
                "        \"includeExistingFiles\": true,\n" +
                "        \"isEnabled\": false,\n" +
                "        \"priority\": 3,\n" +
                "        \"replicationRuleName\": \"my-replication-rule\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"destinationBucketId\": \"777011112222333344445555\",\n" +
                "        \"fileNamePrefix\": \"abc\",\n" +
                "        \"includeExistingFiles\": false,\n" +
                "        \"isEnabled\": true,\n" +
                "        \"priority\": 1,\n" +
                "        \"replicationRuleName\": \"my-replication-rule-2\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"sourceApplicationKeyId\": \"123a0a1a2a3a4a50000bc614e\"\n" +
                "  }\n" +
                "}";

        final B2BucketReplicationConfiguration converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2BucketReplicationConfiguration.class
                );

        final List<B2ReplicationRule> replicationRules = new ArrayList<>();
        replicationRules.add(
                new B2ReplicationRule(
                        "my-replication-rule",
                        "000011112222333344445555",
                        3,
                        "",
                        false,
                        true
                )
        );
        replicationRules.add(
                new B2ReplicationRule(
                        "my-replication-rule-2",
                        "777011112222333344445555",
                        1,
                        "abc",
                        true,
                        false
                )
        );
        final B2BucketReplicationConfiguration defaultConfig =
                B2BucketReplicationConfiguration.createForSource(
                        "123a0a1a2a3a4a50000bc614e",
                        replicationRules
                );
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testToJsonAndBack_destinationOnly() {
        final String jsonString = "{\n" +
                "  \"asReplicationDestination\": {\n" +
                "    \"sourceToDestinationKeyMapping\": {\n" +
                "      \"123a0a1a2a3a4a50000bc614e\": \"555a0a1a2a3a4a70000bc929a\",\n" +
                "      \"456a0b9a8a7a6a50000fc614e\": \"555a0a1a2a3a4a70000bc929a\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"asReplicationSource\": null\n" +
                "}";

        final B2BucketReplicationConfiguration converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2BucketReplicationConfiguration.class
                );

        // Note: the following needs to be a TreeMap in order to be a deterministic test
        final Map<String, String> sourceToDestinationKeyMapping = new TreeMap<>();
        sourceToDestinationKeyMapping.put(
                "123a0a1a2a3a4a50000bc614e", "555a0a1a2a3a4a70000bc929a"
        );
        sourceToDestinationKeyMapping.put(
                "456a0b9a8a7a6a50000fc614e", "555a0a1a2a3a4a70000bc929a"
        );
        final B2BucketReplicationConfiguration defaultConfig =
                B2BucketReplicationConfiguration.createForDestination(
                       sourceToDestinationKeyMapping
                );
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testToJsonAndBack_sourceAndDestination() {
        final String jsonString = "{\n" +
                "  \"asReplicationDestination\": {\n" +
                "    \"sourceToDestinationKeyMapping\": {\n" +
                "      \"123a0a1a2a3a4a50000bc614e\": \"555a0a1a2a3a4a70000bc929a\",\n" +
                "      \"456a0b9a8a7a6a50000fc614e\": \"555a0a1a2a3a4a70000bc929a\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"asReplicationSource\": {\n" +
                "    \"replicationRules\": [\n" +
                "      {\n" +
                "        \"destinationBucketId\": \"000011112222333344445555\",\n" +
                "        \"fileNamePrefix\": \"\",\n" +
                "        \"includeExistingFiles\": true,\n" +
                "        \"isEnabled\": false,\n" +
                "        \"priority\": 3,\n" +
                "        \"replicationRuleName\": \"my-replication-rule\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"destinationBucketId\": \"777011112222333344445555\",\n" +
                "        \"fileNamePrefix\": \"abc\",\n" +
                "        \"includeExistingFiles\": false,\n" +
                "        \"isEnabled\": true,\n" +
                "        \"priority\": 1,\n" +
                "        \"replicationRuleName\": \"my-replication-rule-2\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"sourceApplicationKeyId\": \"123a0a1a2a3a4a50000bc614e\"\n" +
                "  }\n" +
                "}";

        final B2BucketReplicationConfiguration converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2BucketReplicationConfiguration.class
                );

        final List<B2ReplicationRule> replicationRules = listOf(
                new B2ReplicationRule(
                        "my-replication-rule",
                        "000011112222333344445555",
                        3,
                        "",
                        false,
                        true
                ),
                new B2ReplicationRule(
                        "my-replication-rule-2",
                        "777011112222333344445555",
                        1,
                        "abc",
                        true,
                        false
                )
        );

        final Map<String, String> sourceToDestinationKeyMapping = mapOf(
                "123a0a1a2a3a4a50000bc614e", "555a0a1a2a3a4a70000bc929a",
                "456a0b9a8a7a6a50000fc614e", "555a0a1a2a3a4a70000bc929a"
        );

        final B2BucketReplicationConfiguration defaultConfig =
                B2BucketReplicationConfiguration.createForSourceAndDestination(
                        "123a0a1a2a3a4a50000bc614e",
                        replicationRules,
                        sourceToDestinationKeyMapping
                );
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testValueNone() {
        final B2BucketReplicationConfiguration b2BucketReplicationConfiguration =
                B2BucketReplicationConfiguration.NONE;
        
        assertNull(b2BucketReplicationConfiguration.getReplicationRulesOrNull());
        assertNull(b2BucketReplicationConfiguration.getSourceApplicationKeyIdOrNull());
        assertNull(b2BucketReplicationConfiguration.getSourceToDestinationKeyMappingOrNull());
        assertEquals(b2BucketReplicationConfiguration, B2BucketReplicationConfiguration.NONE);
    }
}