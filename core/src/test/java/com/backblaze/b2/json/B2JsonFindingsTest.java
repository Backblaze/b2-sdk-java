/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class B2JsonFindingsTest extends B2BaseTest {
    public static class Versioned {
        @B2Json.versionRange(firstVersion = 4, lastVersion = 6)
        @B2Json.required
        public final int x;

        @B2Json.ignored
        public final int version;

        @B2Json.constructor(params = "x, v", versionParam = "v")
        public Versioned(int x, int v) {
            this.x = x;
            this.version = v;
        }
    }

    /** Finding C26: building a versioned type from a URL parameter map must work (argument list is too short today). */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC26_versionedTypeFromUrlParameterMap() throws Exception {
        final B2JsonOptions options = B2JsonOptions.builder().setVersion(5).build();
        final Versioned v;
        try {
            v = B2Json.get().fromUrlParameterMap(Collections.singletonMap("x", "7"), Versioned.class, options);
        } catch (RuntimeException | B2JsonException e) {
            fail("could not build a versioned type from url parameters: " + e);
            return;
        }
        assertEquals(7, v.x);
        assertEquals(5, v.version);
    }

    /** Finding C27: an unknown field must be rejected whatever its position; a later discarded field must not hide it. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC27_unknownFieldBeforeDiscardedFieldStillRejected() {
        // "tail" is unknown; "breed" is a field Cat is declared to discard.
        final String json = "{ \"type\": \"cat\", \"tail\": \"long\", \"breed\": \"siamese\", \"name\": \"Charlie\" }";
        try {
            final Object pet = B2Json.get().fromJson(json, B2JsonUnionBaseHandlerTest.Pet.class, B2JsonOptions.DEFAULT);
            fail("unknown field 'tail' was silently accepted: " + pet);
        } catch (B2JsonException expected) {
            // expected
        }
    }

    /** Finding C19: hostile deeply nested input must fail cleanly (B2JsonException), not blow the stack. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC19_deeplyNestedInputFailsCleanly() throws IOException {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200_000; i++) {
            sb.append('[');
        }
        final B2JsonReader reader = new B2JsonReader(new StringReader(sb.toString()));
        try {
            reader.skipValue();
            fail("expected an error");
        } catch (B2JsonException expected) {
            // expected
        } catch (StackOverflowError e) {
            fail("StackOverflowError: the JSON skipper has no depth limit");
        }
    }
}
