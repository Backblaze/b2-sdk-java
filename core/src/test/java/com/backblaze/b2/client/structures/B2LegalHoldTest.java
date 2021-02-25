/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.contentSources.B2HeadersImpl;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class B2LegalHoldTest extends B2BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testNullStatus() {
        thrown.expect(IllegalArgumentException.class);
        new B2LegalHold(null);
    }

    @Test
    public void testStatusOn() {
        final String jsonString =
                "{\n" +
                "  \"status\": \"on\"\n" +
                "}";
        final B2LegalHold legalHold = B2Json.fromJsonOrThrowRuntime(jsonString, B2LegalHold.class);
        verifyB2LegalHoldResponseHelper(legalHold, jsonString);
    }

    @Test
    public void testStatusOff() {
        final String jsonString =
                "{\n" +
                        "  \"status\": \"off\"\n" +
                        "}";
        final B2LegalHold legalHold = B2Json.fromJsonOrThrowRuntime(jsonString, B2LegalHold.class);
        verifyB2LegalHoldResponseHelper(legalHold, jsonString);
    }

    @Test
    public void testStatusUnset() {
        final String jsonString =
                "{\n" +
                        "  \"status\": \"unset\"\n" +
                        "}";
        final B2LegalHold legalHold = B2Json.fromJsonOrThrowRuntime(jsonString, B2LegalHold.class);
        verifyB2LegalHoldResponseHelper(legalHold, jsonString);
    }

    @Test
    public void testGetLegalHoldFromHeadersOrNull() {
        final B2Headers b2Headers = B2HeadersImpl.builder()
                .set("X-Bz-File-Lock-Legal-Hold", "off")
                .build();
        final B2LegalHold b2LegalHold = B2LegalHold.getLegalHoldFromHeadersOrNull(b2Headers);
        assertEquals(new B2LegalHold("off"), b2LegalHold);

        final B2Headers b2HeadersNull = null;
        final B2LegalHold b2LegalHold1 = B2LegalHold.getLegalHoldFromHeadersOrNull(b2HeadersNull);
        assertNull(b2LegalHold1);
    }

    private void verifyB2LegalHoldResponseHelper(B2LegalHold response, String jsonString) {
        final String jsonResponse = B2Json.toJsonOrThrowRuntime(response);
        // ensure jsonString is de-serializable
        B2Json.fromJsonOrThrowRuntime(jsonString, B2LegalHold.class);
        final B2LegalHold convertedResponse = B2Json.fromJsonOrThrowRuntime(
                jsonResponse, B2LegalHold.class);

        Assert.assertEquals(response, convertedResponse);
        Assert.assertEquals(jsonString, jsonResponse);
    }
}
