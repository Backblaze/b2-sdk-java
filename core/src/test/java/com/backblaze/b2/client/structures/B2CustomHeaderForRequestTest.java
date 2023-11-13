/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.json.B2JsonOptions;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.TreeSet;

import static com.backblaze.b2.util.B2Collections.listOf;
import static org.junit.Assert.assertEquals;

public class B2CustomHeaderForRequestTest extends B2BaseTest {

    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"name\": \"name1\",\n" +
                "  \"value\": \"val1\"\n" +
                "}";
        final B2CustomHeaderForRequest converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2CustomHeaderForRequest.class,
                        B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS    // for targetType
                );
        final B2CustomHeaderForRequest defaultConfig =
                new B2CustomHeaderForRequest("name1", "val1");
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }

    @Test
    public void testConvertToTreeSetOfB2CustomHeaderForRequest() {
        final TreeSet<B2CustomHeaderForResponse> original = new TreeSet<>(
                listOf(new B2CustomHeaderForResponse("name1", "val1"))
        );

        final TreeSet<B2CustomHeaderForRequest> converted =
                B2CustomHeaderForRequest.convertToTreeSetOfB2CustomHeaderForRequest(original);

        final TreeSet<B2CustomHeaderForRequest> expected = new TreeSet<>(
                listOf(new B2CustomHeaderForRequest("name1", "val1"))
        );

        assertEquals(expected, converted);
    }
}
