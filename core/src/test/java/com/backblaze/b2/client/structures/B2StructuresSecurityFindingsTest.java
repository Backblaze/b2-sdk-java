/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.B2TestHelpers;
import com.backblaze.b2.client.exceptions.B2SignatureVerificationException;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/** Abuse-case tests for C15 and C33. */
public class B2StructuresSecurityFindingsTest extends B2BaseTest {
    private static final String SECRET = "3XDfkdQte2OgA78qCtSD17LAzpj6ay9H";
    private static final String PAYLOAD = "{\"events\": []}";

    /** Finding C15 (security): toString() of the account authorization must not contain the authorization token. */
    @Test
    public void testFindingC15_accountAuthorizationToStringHidesToken() {
        final B2AccountAuthorization auth = B2TestHelpers.makeAuth(1);
        assertFalse("token printed: " + auth, auth.toString().contains(auth.getAuthorizationToken()));
    }

    /** Finding C15 (security): toString() of an SSE-C request must not contain the customer key. */
    @Test
    public void testFindingC15_sseForRequestToStringHidesCustomerKey() {
        final String key = java.util.Base64.getEncoder().encodeToString("ThisIsA32ByteSecretKeyForTesting!".getBytes(StandardCharsets.UTF_8));
        final B2FileSseForRequest sse = B2FileSseForRequest.createSseCAes256(key);
        assertFalse("customer key printed: " + sse, sse.toString().contains(key));
    }

    /** Finding C33 (security): when a signing secret is configured, an event without a signature header must be rejected. */
    @Test
    public void testFindingC33_missingSignatureWithSecretIsRejected_string() throws Exception {
        try {
            final B2EventNotification parsed = B2EventNotification.parse(PAYLOAD, null, SECRET);
            fail("unsigned event was accepted: " + parsed);
        } catch (B2SignatureVerificationException expected) {
            // expected
        }
    }

    /** Finding C33 (security): same for the byte[] overload. */
    @Test
    public void testFindingC33_missingSignatureWithSecretIsRejected_bytes() throws Exception {
        try {
            final B2EventNotification parsed = B2EventNotification.parse(PAYLOAD.getBytes(StandardCharsets.UTF_8), null, SECRET);
            fail("unsigned event was accepted: " + parsed);
        } catch (B2SignatureVerificationException expected) {
            // expected
        }
    }
}
