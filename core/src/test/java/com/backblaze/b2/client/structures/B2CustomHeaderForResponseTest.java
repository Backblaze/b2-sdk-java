/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.json.B2JsonOptions;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2CustomHeaderForResponseTest extends B2BaseTest {

    @Test
    public void testToJsonAndBack() {
        final String jsonString = "{\n" +
                "  \"name\": \"name1\",\n" +
                "  \"value\": \"val1\"\n" +
                "}";
        final B2CustomHeaderForResponse converted =
                B2Json.fromJsonOrThrowRuntime(
                        jsonString,
                        B2CustomHeaderForResponse.class,
                        B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS    // for targetType
                );
        final B2CustomHeaderForResponse defaultConfig =
                new B2CustomHeaderForResponse("name1", "val1");
        final String convertedJson = B2Json.toJsonOrThrowRuntime(defaultConfig);
        assertEquals(defaultConfig, converted);
        assertEquals(jsonString, convertedJson);
    }
}
