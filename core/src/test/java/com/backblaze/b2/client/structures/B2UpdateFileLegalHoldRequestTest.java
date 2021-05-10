/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class B2UpdateFileLegalHoldRequestTest extends B2BaseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testLegalHoldOn() {
        final B2UpdateFileLegalHoldRequest request = B2UpdateFileLegalHoldRequest
                .builder("fluffy.jpg", "file-id", B2LegalHold.ON)
                .build();
        final String requestJson = B2Json.toJsonOrThrowRuntime(request);

        final String expectedJson = "{\n" +
                "  \"fileId\": \"file-id\",\n" +
                "  \"fileName\": \"fluffy.jpg\",\n" +
                "  \"legalHold\": \"on\"\n" +
                "}";

        assertEquals(expectedJson, requestJson);
    }

    @Test
    public void testLegalHoldOff() {
        final B2UpdateFileLegalHoldRequest request = B2UpdateFileLegalHoldRequest
                .builder("fluffy.jpg", "file-id", B2LegalHold.OFF)
                .build();
        final String requestJson = B2Json.toJsonOrThrowRuntime(request);

        final String expectedJson = "{\n" +
                "  \"fileId\": \"file-id\",\n" +
                "  \"fileName\": \"fluffy.jpg\",\n" +
                "  \"legalHold\": \"off\"\n" +
                "}";

        assertEquals(expectedJson, requestJson);
    }

    @Test
    public void testInvalidLegalHoldThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid legalHold value. Valid values: on, off");
        //noinspection unused
        final B2UpdateFileLegalHoldRequest ignored = B2UpdateFileLegalHoldRequest
                .builder("fluffy.jpg", "file-id", "neither")
                .build();
    }

    @Test
    public void testNullLegalHoldThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid legalHold value. Valid values: on, off");
        //noinspection unused
        final B2UpdateFileLegalHoldRequest ignored = B2UpdateFileLegalHoldRequest
                .builder("fluffy.jpg", "file-id", null)
                .build();
    }
}