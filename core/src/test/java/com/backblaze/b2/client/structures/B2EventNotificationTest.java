package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.exceptions.B2SignatureVerificationException;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.json.B2JsonException;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;


public class B2EventNotificationTest extends B2BaseTest {

    private static final String HMAC_SHA256_SIGNING_SECRET = "3XDfkdQte2OgA78qCtSD17LAzpj6ay9H";
    private static final String HMAC_SHA256_SIGNING_SECRET2 = "rrzaVL6BqYt83s2Q5R2I79AilaxVBJUS";

    private static final String DEFAULT_EVENT_PAYLOAD = "{\n" +
            "  \"events\": [\n" +
            "    {\n" +
            "      \"accountId\": \"e85c6a500333\",\n" +
            "      \"bucketId\": \"aea8c5bc362ae55070130333\",\n" +
            "      \"bucketName\": \"mySampleBucket\",\n" +
            "      \"eventId\": \"eventId\",\n" +
            "      \"eventTimestamp\": 1684793309123,\n" +
            "      \"eventType\": \"b2:ObjectCreated:Upload\",\n" +
            "      \"eventVersion\": 1,\n" +
            "      \"matchedRuleName\": \"mySampleRule1\",\n" +
            "      \"objectName\": \"objectName.txt\",\n" +
            "      \"objectSize\": 10495842,\n" +
            "      \"objectVersionId\": \"4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    public static final byte[] DEFAULT_EVENT_BYTES =
            DEFAULT_EVENT_PAYLOAD.getBytes(StandardCharsets.UTF_8);

    @Test
    public void testToJsonAndBack() {
        final B2EventNotification eventNotification =
                B2Json.fromJsonOrThrowRuntime(
                        DEFAULT_EVENT_PAYLOAD,
                        B2EventNotification.class
                );

        final List<B2EventNotificationEvent> expectedEvents = new ArrayList<>();
        expectedEvents.add(new B2EventNotificationEvent(
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
                "4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235"));

        final B2EventNotification expectedNotification = new B2EventNotification(expectedEvents);

        final String convertedJson = B2Json.toJsonOrThrowRuntime(expectedNotification);

        assertEquals(DEFAULT_EVENT_PAYLOAD, convertedJson);
        assertEquals(expectedNotification, eventNotification);
    }

    @Test
    public void testToJsonAndBackWithNullSize() {
        final String jsonString = "{\n" +
                "  \"events\": [\n" +
                "    {\n" +
                "      \"accountId\": \"e85c6a500333\",\n" +
                "      \"bucketId\": \"aea8c5bc362ae55070130333\",\n" +
                "      \"bucketName\": \"mySampleBucket\",\n" +
                "      \"eventId\": \"eventId\",\n" +
                "      \"eventTimestamp\": 1684793309123,\n" +
                "      \"eventType\": \"b2:ObjectCreated:Upload\",\n" +
                "      \"eventVersion\": 1,\n" +
                "      \"matchedRuleName\": \"mySampleRule1\",\n" +
                "      \"objectName\": \"objectName.txt\",\n" +
                "      \"objectVersionId\": \"4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        final B2EventNotification eventNotification =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2EventNotification.class
                );

        final List<B2EventNotificationEvent> expectedEvents = new ArrayList<>();
        expectedEvents.add(new B2EventNotificationEvent(
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
                "4_zaea8c5bc362ae55070130333_f117c7bd5d6c6597c_d20230521_m235957_c001_v0001044_t0052_u01684713597235"));

        final B2EventNotification expectedNotification = new B2EventNotification(expectedEvents);

        final String convertedJson = B2Json.toJsonOrThrowRuntime(expectedNotification);

        assertEquals(jsonString, convertedJson);
        assertEquals(expectedNotification, eventNotification);
    }

    @Test
    public void testParseSuccess() throws B2JsonException, B2SignatureVerificationException {
        final String signature = B2EventNotification.SignatureUtils.computeHmacSha256Signature(HMAC_SHA256_SIGNING_SECRET, DEFAULT_EVENT_PAYLOAD.getBytes(StandardCharsets.UTF_8));
        final B2EventNotification b2EventNotification = B2EventNotification.parse(DEFAULT_EVENT_PAYLOAD, signature, HMAC_SHA256_SIGNING_SECRET);
        final B2EventNotification parsedEventNotification = B2Json.get().fromJson(DEFAULT_EVENT_PAYLOAD, B2EventNotification.class);
        assertEquals(parsedEventNotification, b2EventNotification);
    }

    @Test
    public void testParseSuccessWithNoSignature() throws B2JsonException, B2SignatureVerificationException {
        final B2EventNotification b2EventNotification = B2EventNotification.parse(DEFAULT_EVENT_PAYLOAD, null, HMAC_SHA256_SIGNING_SECRET);
        final B2EventNotification parsedEventNotification = B2Json.get().fromJson(DEFAULT_EVENT_PAYLOAD, B2EventNotification.class);
        assertEquals(parsedEventNotification, b2EventNotification);
    }

    @Test
    public void testParseSuccessWithNoSecret() throws B2JsonException, B2SignatureVerificationException {
        final String signature = B2EventNotification.SignatureUtils.computeHmacSha256Signature(HMAC_SHA256_SIGNING_SECRET, DEFAULT_EVENT_PAYLOAD.getBytes(StandardCharsets.UTF_8));
        final B2EventNotification b2EventNotification = B2EventNotification.parse(DEFAULT_EVENT_PAYLOAD, signature, null);
        final B2EventNotification parsedEventNotification = B2Json.get().fromJson(DEFAULT_EVENT_PAYLOAD, B2EventNotification.class);
        assertEquals(parsedEventNotification, b2EventNotification);
    }

    @Test
    public void testParseSuccessWithNoSignatureOrSecret() throws B2JsonException, B2SignatureVerificationException {
        final B2EventNotification b2EventNotification = B2EventNotification.parse(DEFAULT_EVENT_PAYLOAD, null, null);
        final B2EventNotification parsedEventNotification = B2Json.get().fromJson(DEFAULT_EVENT_PAYLOAD, B2EventNotification.class);
        assertEquals(parsedEventNotification, b2EventNotification);
    }

    @Test
    public void testParseSuccessWithNoSignatureValidation() throws B2JsonException {
        final B2EventNotification b2EventNotification = B2EventNotification.parse(DEFAULT_EVENT_PAYLOAD);
        final B2EventNotification parsedEventNotification = B2Json.get().fromJson(DEFAULT_EVENT_PAYLOAD, B2EventNotification.class);
        assertEquals(parsedEventNotification, b2EventNotification);
    }

    @Test
    public void testParseSignatureFailure() throws B2SignatureVerificationException {
        final String signature = B2EventNotification.SignatureUtils.computeHmacSha256Signature(HMAC_SHA256_SIGNING_SECRET2, DEFAULT_EVENT_PAYLOAD.getBytes(StandardCharsets.UTF_8));
        assertThrows(B2SignatureVerificationException.class,() -> B2EventNotification.parse(DEFAULT_EVENT_PAYLOAD, signature, HMAC_SHA256_SIGNING_SECRET));
    }

    @Test
    public void testParseBytesSuccess() throws B2JsonException, IOException, B2SignatureVerificationException {
        final String signature = B2EventNotification.SignatureUtils.computeHmacSha256Signature(HMAC_SHA256_SIGNING_SECRET, DEFAULT_EVENT_BYTES);
        final B2EventNotification b2EventNotification = B2EventNotification.parse(DEFAULT_EVENT_BYTES, signature, HMAC_SHA256_SIGNING_SECRET);
        final B2EventNotification parsedEventNotification = B2Json.get().fromJson(DEFAULT_EVENT_PAYLOAD, B2EventNotification.class);
        assertEquals(parsedEventNotification, b2EventNotification);
    }

    @Test
    public void testParseBytesSuccessWithNoSignature() throws B2JsonException, IOException, B2SignatureVerificationException {
        final B2EventNotification b2EventNotification = B2EventNotification.parse(DEFAULT_EVENT_BYTES, null, HMAC_SHA256_SIGNING_SECRET);
        final B2EventNotification parsedEventNotification = B2Json.get().fromJson(DEFAULT_EVENT_PAYLOAD, B2EventNotification.class);
        assertEquals(parsedEventNotification, b2EventNotification);
    }

    @Test
    public void testParseBytesSuccessWithNoSecret() throws B2JsonException, IOException, B2SignatureVerificationException {
        final String signature = B2EventNotification.SignatureUtils.computeHmacSha256Signature(HMAC_SHA256_SIGNING_SECRET, DEFAULT_EVENT_PAYLOAD.getBytes(StandardCharsets.UTF_8));
        final B2EventNotification b2EventNotification = B2EventNotification.parse(DEFAULT_EVENT_BYTES, signature, null);
        final B2EventNotification parsedEventNotification = B2Json.get().fromJson(DEFAULT_EVENT_PAYLOAD, B2EventNotification.class);
        assertEquals(parsedEventNotification, b2EventNotification);
    }

    @Test
    public void testParseBytesSuccessWithNoSignatureOrSecret() throws B2JsonException, IOException, B2SignatureVerificationException {
        final B2EventNotification b2EventNotification = B2EventNotification.parse(DEFAULT_EVENT_BYTES, null, null);
        final B2EventNotification parsedEventNotification = B2Json.get().fromJson(DEFAULT_EVENT_PAYLOAD, B2EventNotification.class);
        assertEquals(parsedEventNotification, b2EventNotification);
    }

    @Test
    public void testParseBytesSuccessWithNoSignatureValidation() throws B2JsonException, IOException {
        final B2EventNotification b2EventNotification = B2EventNotification.parse(DEFAULT_EVENT_BYTES);
        final B2EventNotification parsedEventNotification = B2Json.get().fromJson(DEFAULT_EVENT_PAYLOAD, B2EventNotification.class);
        assertEquals(parsedEventNotification, b2EventNotification);
    }

    @Test
    public void testParseWithInvalidSignature() throws B2SignatureVerificationException {
        final byte[] jsonBytes = DEFAULT_EVENT_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        final String signature = B2EventNotification.SignatureUtils.computeHmacSha256Signature(HMAC_SHA256_SIGNING_SECRET,
                "HelloWorld".getBytes(StandardCharsets.UTF_8));
        assertThrows(B2SignatureVerificationException.class, () -> B2EventNotification.parse(jsonBytes, signature, HMAC_SHA256_SIGNING_SECRET));
    }

    @Test
    public void testParseWithInvalidSecret() throws B2SignatureVerificationException {
        final byte[] jsonBytes = DEFAULT_EVENT_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        final String signature = B2EventNotification.SignatureUtils.computeHmacSha256Signature(HMAC_SHA256_SIGNING_SECRET,
                jsonBytes);
        assertThrows(B2SignatureVerificationException.class, () -> B2EventNotification.parse(jsonBytes, signature, "Hello World"));
    }

    @Test
    public void testParseWithInvalidParameters() throws B2SignatureVerificationException {
        final byte[] jsonBytes = DEFAULT_EVENT_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        final String signature = B2EventNotification.SignatureUtils.computeHmacSha256Signature(HMAC_SHA256_SIGNING_SECRET, jsonBytes);

        assertThrows(IllegalArgumentException.class, () -> B2EventNotification.parse((byte[])null));
        assertThrows(IllegalArgumentException.class, () -> B2EventNotification.parse((byte[])null, signature, HMAC_SHA256_SIGNING_SECRET));
        assertThrows(IllegalArgumentException.class, () -> B2EventNotification.parse(new byte[0], signature, HMAC_SHA256_SIGNING_SECRET));

        assertThrows(IllegalArgumentException.class, () -> B2EventNotification.parse((String)null));
        assertThrows(IllegalArgumentException.class, () -> B2EventNotification.parse((String)null, signature, HMAC_SHA256_SIGNING_SECRET));
        assertThrows(IllegalArgumentException.class, () -> B2EventNotification.parse("", signature, HMAC_SHA256_SIGNING_SECRET));
    }

    @Test
    public void testParseWithInvalidJson() throws B2SignatureVerificationException {
        final byte[] jsonBytes = "{\n\"key\":\"value\"\n}".getBytes(StandardCharsets.UTF_8);
        final String signature = B2EventNotification.SignatureUtils.computeHmacSha256Signature(HMAC_SHA256_SIGNING_SECRET,
                jsonBytes);
        assertThrows(B2JsonException.class, () -> B2EventNotification.parse(jsonBytes, signature, HMAC_SHA256_SIGNING_SECRET));
    }
}
