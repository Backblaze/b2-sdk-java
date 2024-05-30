package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class B2EventNotificationEventTest extends B2BaseTest {

    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"accountId\": \"e85c6a500333\",\n" +
                "  \"bucketId\": \"aea8c5bc362ae55070130333\",\n" +
                "  \"bucketName\": \"mySampleBucket\",\n" +
                "  \"eventId\": \"eventId\",\n" +
                "  \"eventTimestamp\": 1684793309123,\n" +
                "  \"eventType\": \"b2:ObjectCreated:Upload\",\n" +
                "  \"eventVersion\": 1,\n" +
                "  \"matchedRuleName\": \"mySampleRule1\",\n" +
                "  \"objectName\": \"objectName.txt\",\n" +
                "  \"objectSize\": 10495842,\n" +
                "  \"objectVersionId\": \"4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235\"\n" +
                "}";
        final B2EventNotificationEvent event =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2EventNotificationEvent.class
                );

        final B2EventNotificationEvent expectedEvent = new B2EventNotificationEvent(
                "e85c6a500333",
                "aea8c5bc362ae55070130333",
                "mySampleBucket",
                1684793309123L,
                "b2:ObjectCreated:Upload",
                "eventId",
                1,
                "mySampleRule1",
                "objectName.txt",
                10495842L,
                "4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235"
        );
        final String convertedJson = B2Json.toJsonOrThrowRuntime(expectedEvent);
        assertEquals(expectedEvent, event);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testToJsonAndBack_testEvent() {
        final String jsonString = "{\n" +
                "  \"accountId\": \"e85c6a500333\",\n" +
                "  \"bucketId\": \"aea8c5bc362ae55070130333\",\n" +
                "  \"bucketName\": \"mySampleBucket\",\n" +
                "  \"eventId\": \"eventId\",\n" +
                "  \"eventTimestamp\": 1684793309123,\n" +
                "  \"eventType\": \"b2:TestEvent\",\n" +
                "  \"eventVersion\": 1,\n" +
                "  \"matchedRuleName\": \"mySampleRule1\"\n" +
                "}";
        final B2EventNotificationEvent event =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2EventNotificationEvent.class
                );

        final B2EventNotificationEvent expectedEvent = new B2EventNotificationEvent(
                "e85c6a500333",
                "aea8c5bc362ae55070130333",
                "mySampleBucket",
                1684793309123L,
                "b2:TestEvent",
                "eventId",
                1,
                "mySampleRule1",
                null,
                null,
                null
        );
        final String convertedJson = B2Json.toJsonOrThrowRuntime(expectedEvent);
        assertEquals(expectedEvent, event);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testTestEventsMustNotHaveObjectName() {
        final IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> new B2EventNotificationEvent(
                        "e85c6a500333",
                        "aea8c5bc362ae55070130333",
                        "mySampleBucket",
                        1684793309123L,
                        "b2:TestEvent",
                        "eventId",
                        1,
                        "mySampleRule1",
                        "objectName.txt",
                        null,
                        null
                )
        );
        assertEquals(illegalArgumentException.getMessage(), "objectName must be null for test events");
    }

    @Test
    public void testTestEventsMustNotHaveObjectSize() {
        final IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> new B2EventNotificationEvent(
                        "e85c6a500333",
                        "aea8c5bc362ae55070130333",
                        "mySampleBucket",
                        1684793309123L,
                        "b2:TestEvent",
                        "eventId",
                        1,
                        "mySampleRule1",
                        null,
                        10495842L,
                        null
                )
        );
        assertEquals(illegalArgumentException.getMessage(), "objectSize must be null for test events");
    }

    @Test
    public void testTestEventsMustNotHaveObjectVersionId() {
        final IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> new B2EventNotificationEvent(
                        "e85c6a500333",
                        "aea8c5bc362ae55070130333",
                        "mySampleBucket",
                        1684793309123L,
                        "b2:TestEvent",
                        "eventId",
                        1,
                        "mySampleRule1",
                        null,
                        null,
                        "4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235"
                )
        );
        assertEquals(illegalArgumentException.getMessage(), "objectVersionId must be null for test events");
    }

    @Test
    public void testNonTestEventsMustHaveObjectName() {
        final IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> new B2EventNotificationEvent(
                        "e85c6a500333",
                        "aea8c5bc362ae55070130333",
                        "mySampleBucket",
                        1684793309123L,
                        "b2:ObjectCreated:Upload",
                        "eventId",
                        1,
                        "mySampleRule1",
                        null,
                        null,
                        "4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235"
                )
        );
        assertEquals(illegalArgumentException.getMessage(), "objectName is required");
    }

    @Test
    public void testNonTestEventsMustHaveObjectVersionId() {
        final IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> new B2EventNotificationEvent(
                        "e85c6a500333",
                        "aea8c5bc362ae55070130333",
                        "mySampleBucket",
                        1684793309123L,
                        "b2:ObjectCreated:Upload",
                        "eventId",
                        1,
                        "mySampleRule1",
                        "objectName.txt",
                        null,
                        null
                )
        );
        assertEquals(illegalArgumentException.getMessage(), "objectVersionId is required");
    }
}
