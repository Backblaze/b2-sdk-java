/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.exceptions.B2RuntimeException;
import com.backblaze.b2.client.exceptions.B2SignatureVerificationException;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.json.B2JsonException;
import com.backblaze.b2.util.B2StringUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The notification that Backblaze sends when object events occur.
 */
public class B2EventNotification {

    @B2Json.required
    private final List<B2EventNotificationEvent> events;

    @B2Json.constructor
    public B2EventNotification(List<B2EventNotificationEvent> events) {
        this.events = events;
    }

    /**
     * A list of object events.
     */
    public List<B2EventNotificationEvent> getEvents() {
        return events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof B2EventNotification)) return false;
        final B2EventNotification that = (B2EventNotification) o;
        return Objects.equals(events, that.events);
    }

    @Override
    public int hashCode() {
        return Objects.hash(events);
    }

    @Override
    public String toString() {
        return "B2EventNotification{" +
                "events=" + events +
                '}';
    }

    /**
     * Construct a new EventNotification from JSON content
     * @param json - The JSON content to create an B2EventNotification object from. The byte array should be UTF-8 encoded
     * @param signatureFromHeader - The value of the x-bz-header
     * @param signingSecret - The secret for computing the signature.
     * @return - The B2EventNotification objects with attributes as in the JSON content
     * @throws B2JsonException - If the content is not valid JSON
     * @throws B2SignatureVerificationException - if the content does not match the signature from header.
     */
    public static B2EventNotification constructEventNotification(byte[] json, String signatureFromHeader, String signingSecret) throws B2JsonException, IOException, B2SignatureVerificationException {
        SignatureUtils.verifySignature(json, signatureFromHeader, signingSecret);
        return B2Json.get().fromJson(json , B2EventNotification.class);
    }

    /**
     * Construct a new EventNotification from JSON content
     * @param json - The JSON content to create an B2EventNotification object from.
     * @param signatureFromHeader - The value of the x-bz-header
     * @param signingSecret - The secret for computing the signature.
     * @return - The B2EventNotification objects with attributes as in the JSON content
     * @throws B2JsonException - If the content is not valid JSON
     * @throws B2SignatureVerificationException - if the content does not match the signature from header.
     */
    public static B2EventNotification constructEventNotification(String json, String signatureFromHeader, String signingSecret) throws B2JsonException, B2SignatureVerificationException {
        SignatureUtils.verifySignature(json.getBytes(StandardCharsets.UTF_8), signatureFromHeader, signingSecret);
        return B2Json.get().fromJson(json, B2EventNotification.class);
    }

    /**
     * Utility private class to encapsulate signature verification.
     */
    static class SignatureUtils {
        public static void verifySignature(byte[] json, String signatureFromHeader, String signingSecret) throws B2SignatureVerificationException {
            final String[] signatures = signatureFromHeader.split(",");
            final String signature = computeHmacSha256Signature(signingSecret, json);

            boolean signatureMatch = Arrays.stream(signatures).anyMatch(signatureToVerify -> Objects.equals(signatureToVerify, signature));
            if (!signatureMatch) {
                throw new B2SignatureVerificationException("Signature from header does not match calculated signature");
            }
        }

        static String computeHmacSha256Signature(String signingSecret,
                                                 byte[] b2JsonSerializableObject) {
            final byte[] hmacSha256Signature = sign(signingSecret.getBytes(StandardCharsets.UTF_8), b2JsonSerializableObject);
            return "v1=" + B2StringUtil.toHexString(hmacSha256Signature);
        }

        private static byte[] sign(byte[] key, byte[] data) {
            final SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA256");
            final Mac mac = getMac();
            try {
                mac.init(secretKey);
            } catch (InvalidKeyException e) {
                throw new RuntimeException("Invalid key for HmacSHA256", e);
            }
            return mac.doFinal(data);
        }

        private static Mac getMac() {
            try {
                return Mac.getInstance("HmacSHA256");
            } catch (NoSuchAlgorithmException error) {
                throw new B2RuntimeException("Cannot get HmacSHA256 algorithm which is required to be available");
            }
        }
    }
}
