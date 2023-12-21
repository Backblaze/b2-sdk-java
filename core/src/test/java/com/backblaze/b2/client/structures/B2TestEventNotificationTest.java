package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class B2TestEventNotificationTest extends B2BaseTest {

    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"events\": [\n" +
                "    {\n" +
                "      \"accountId\": \"e85c6a500333\",\n" +
                "      \"bucketId\": \"aea8c5bc362ae55070130333\",\n" +
                "      \"bucketName\": \"mySampleBucket\",\n" +
                "      \"eventTimestamp\": 1684793309123,\n" +
                "      \"eventType\": \"b2:TestEvent\",\n" +
                "      \"eventVersion\": 1,\n" +
                "      \"matchedRuleName\": \"mySampleRule1\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        final B2TestEventNotification testEventNotification =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2TestEventNotification.class
                );

        final List<B2TestEventNotificationEvent> expectedEvents = new ArrayList<>();
        expectedEvents.add(new B2TestEventNotificationEvent(
                "e85c6a500333",
                "aea8c5bc362ae55070130333",
                "mySampleBucket",
                1684793309123L,
                "b2:TestEvent",
                1,
                "mySampleRule1"));

        final B2TestEventNotification expectedNotification = new B2TestEventNotification(expectedEvents);

        final String convertedJson = B2Json.toJsonOrThrowRuntime(expectedNotification);

        assertEquals(jsonString, convertedJson);
        assertEquals(expectedNotification, testEventNotification);
    }
}