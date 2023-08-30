package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2EventNotificationEventTest extends B2BaseTest {

    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"eventTimestamp\": 1684793309123,\n" +
                "  \"eventType\": \"b2:ObjectCreated:Upload\",\n" +
                "  \"objectName\": \"objectName.txt\",\n" +
                "  \"objectSize\": 10495842,\n" +
                "  \"objectVersionId\": \"4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235\"\n" +
                "}";
        final B2EventNotificationEvent event =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2EventNotificationEvent.class
                );

        B2EventNotificationEvent expectedEvent = new B2EventNotificationEvent(1684793309123L,
                "b2:ObjectCreated:Upload",
                "objectName.txt",
                10495842,
                "4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235");
        final String convertedJson = B2Json.toJsonOrThrowRuntime(expectedEvent);
        assertEquals(expectedEvent, event);
        assertEquals(jsonString, convertedJson);
    }
}