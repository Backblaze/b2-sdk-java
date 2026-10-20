// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.exceptions.B2SignatureVerificationException;
import com.backblaze.b2.client.structures.B2EventNotification;
import com.backblaze.b2.client.structures.B2EventNotificationEvent;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * bucket.inbound_webhook. Run by ../run-conformance (see ../../README.md).
 *
 * <p>The X-Bz-Event-Notification-Signature value "comprises the signature version, always v1, followed by an equals
 * sign =, followed by the lowercase hex-encoded signature value", and to verify it "create an HMAC SHA-256 digest
 * using the hmacSha256SigningSecret ... as the key, with the request payload as the data. The resulting hex-encoded
 * HMAC digest value must match the signature in the header." The payload's event fields are the documented ones
 * (https://www.backblaze.com/docs/cloud-storage-event-notifications-reference-guide).
 *
 * <p>The SDK's part of this capability is parsing and verifying what a service receives, so nothing here needs a
 * server round trip: a payload shaped like the documented example is signed in this check with its own HMAC code (not
 * the SDK's), then given to {@code B2EventNotification.parse}. Asserted: with the right secret the payload is accepted
 * and every documented event field is read back; with another secret, with the payload changed after signing, and
 * with a signature that is not v1 hex, it is refused with {@code B2SignatureVerificationException}; a test event (no
 * object fields) parses.
 */
public class BucketInboundWebhook {
    private static final String PAYLOAD = "{\"events\":[{"
            + "\"accountId\":\"sdkharness-account\",\"bucketId\":\"sdkharness-bucket-id\",\"bucketName\":\"sdkharness-conf-bucket\","
            + "\"eventId\":\"sdkharness-event-1\",\"eventTimestamp\":1761000000000,\"eventType\":\"b2:ObjectCreated:Upload\","
            + "\"eventVersion\":1,\"matchedRuleName\":\"sdkharness-rule\",\"objectName\":\"st/new.txt\",\"objectSize\":1234,"
            + "\"objectVersionId\":\"sdkharness-version-1\"}]}";
    private static final String TEST_PAYLOAD = "{\"events\":[{"
            + "\"accountId\":\"sdkharness-account\",\"bucketId\":\"sdkharness-bucket-id\",\"bucketName\":\"sdkharness-conf-bucket\","
            + "\"eventId\":\"sdkharness-event-2\",\"eventTimestamp\":1761000000001,\"eventType\":\"b2:TestEvent\","
            + "\"eventVersion\":1,\"matchedRuleName\":\"sdkharness-rule\"}]}";
    private static final String SECRET = "*******";

    public static void main(String[] args) {
        Support.run("bucket.inbound_webhook", () -> {
            Support.step("accept a correctly signed payload");
            final byte[] body = PAYLOAD.getBytes(StandardCharsets.UTF_8);
            final B2EventNotification accepted = B2EventNotification.parse(body, sign(body, SECRET), SECRET);
            Support.eq("number of events", 1, accepted.getEvents().size());
            final B2EventNotificationEvent event = accepted.getEvents().get(0);
            Support.eq("accountId", "sdkharness-account", event.getAccountId());
            Support.eq("bucketId", "sdkharness-bucket-id", event.getBucketId());
            Support.eq("bucketName", "sdkharness-conf-bucket", event.getBucketName());
            Support.eq("eventId", "sdkharness-event-1", event.getEventId());
            Support.eq("eventTimestamp", 1761000000000L, event.getEventTimestamp());
            Support.eq("eventType", "b2:ObjectCreated:Upload", event.getEventType());
            Support.eq("eventVersion", 1, event.getEventVersion());
            Support.eq("matchedRuleName", "sdkharness-rule", event.getMatchedRuleName());
            Support.eq("objectName", "st/new.txt", event.getObjectName());
            Support.eq("objectSize", 1234L, event.getObjectSize());
            Support.eq("objectVersionId", "sdkharness-version-1", event.getObjectVersionId());

            Support.step("the string overload agrees");
            Support.eq("event id through the string overload", "sdkharness-event-1",
                    B2EventNotification.parse(PAYLOAD, sign(body, SECRET), SECRET).getEvents().get(0).getEventId());

            Support.step("refuse a wrong secret");
            refused("a payload verified with another secret", () -> B2EventNotification.parse(body, sign(body, SECRET), "another-secret"));

            Support.step("refuse a changed payload");
            final byte[] changed = PAYLOAD.replace("st/new.txt", "st/evil.txt").getBytes(StandardCharsets.UTF_8);
            refused("a payload changed after signing", () -> B2EventNotification.parse(changed, sign(body, SECRET), SECRET));

            Support.step("refuse a signature that is not v1 hex");
            refused("a signature without the v1= prefix", () -> B2EventNotification.parse(body, sign(body, SECRET).substring(3), SECRET));
            refused("an empty signature", () -> B2EventNotification.parse(body, "", SECRET));

            Support.step("a test event");
            final byte[] test = TEST_PAYLOAD.getBytes(StandardCharsets.UTF_8);
            final B2EventNotificationEvent testEvent = B2EventNotification.parse(test, sign(test, SECRET), SECRET).getEvents().get(0);
            Support.eq("test event type", "b2:TestEvent", testEvent.getEventType());
            Support.eq("test event object name", null, testEvent.getObjectName());
        });
    }

    private interface Parse {
        void run() throws Exception;
    }

    private static void refused(String what, Parse parse) throws Exception {
        try {
            parse.run();
        } catch (B2SignatureVerificationException expected) {
            return;
        }
        Support.fail(what + " was accepted");
    }

    /** The documented signature: "v1=" and the lowercase hex of HMAC-SHA256(secret, payload). Not the SDK's code. */
    private static String sign(byte[] payload, String secret) throws Exception {
        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        final StringBuilder hex = new StringBuilder("v1=");
        for (byte b : mac.doFinal(payload)) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
