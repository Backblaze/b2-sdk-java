package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class B2EventNotificationTest extends B2BaseTest {
    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"accountId\": \"e85c6a500333\",\n" +
                "  \"bucketId\": \"aea8c5bc362ae55070130333\",\n" +
                "  \"bucketName\": \"mySampleBucket\",\n" +
                "  \"eventVersion\": 1,\n" +
                "  \"events\": [\n" +
                "    {\n" +
                "      \"eventTimestamp\": 1684793309123,\n" +
                "      \"eventType\": \"b2:ObjectCreated:Upload\",\n" +
                "      \"objectName\": \"objectName.txt\",\n" +
                "      \"objectSize\": 10495842,\n" +
                "      \"objectVersionId\": \"4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"matchedRuleName\": \"mySampleRule1\"\n" +
                "}";
        final B2EventNotification eventNotification =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2EventNotification.class
                );

        List<B2EventNotificationEvent> expectedEvents = new ArrayList<>();
        expectedEvents.add(new B2EventNotificationEvent(1684793309123L,
                "b2:ObjectCreated:Upload",
                "objectName.txt",
                10495842,
                "4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235"));

        B2EventNotification expectedNotification = new B2EventNotification(1,
                "mySampleRule1",
                "mySampleBucket",
                "aea8c5bc362ae55070130333",
                "e85c6a500333",
                expectedEvents);

        final String convertedJson = B2Json.toJsonOrThrowRuntime(expectedNotification);

        assertEquals(jsonString, convertedJson);
        assertEquals(expectedNotification, eventNotification);
    }
}