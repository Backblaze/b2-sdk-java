/*
 * Copyright 2024, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.junit.Assert.*;

public class B2JsonDeserializationUtilTest extends B2BaseTest {

    @Test
    public void findConstructor_withJavaRecord() throws B2JsonException {
        final Constructor<B2JsonRecord> constructor = B2JsonDeserializationUtil.findConstructor(B2JsonRecord.class);
        assertNotNull(constructor);
    }

    @Test
    public void findConstructor_withJavaRecordWithoutB2JsonTypeAnnotation() {
        final B2JsonException exception = assertThrows(B2JsonException.class, () -> B2JsonDeserializationUtil.findConstructor(B2JsonRecordWithoutTypeAnnotation.class));
        assertTrue(exception.getMessage().contains("has no constructor annotated with B2Json.constructor"));
    }

    @B2Json.type
    public record B2JsonRecord(@B2Json.required String name) {

    }

    public record B2JsonRecordWithoutTypeAnnotation(@B2Json.required String name) {

    }
}