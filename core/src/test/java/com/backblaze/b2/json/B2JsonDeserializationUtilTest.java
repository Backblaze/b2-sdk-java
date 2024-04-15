/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class B2JsonDeserializationUtilTest extends B2BaseTest {

    @Test
    public void findConstructor_withB2JsonConstructor() throws B2JsonException {
        final Constructor<B2JsonConstructor> constructor = B2JsonDeserializationUtil.findConstructor(B2JsonConstructor.class);
        assertNotNull(constructor);
    }

    @Test
    public void findConstructor_withMultipleConstructorsNoneWithB2Json() {
        final B2JsonException b2JsonException = assertThrows(B2JsonException.class, () -> B2JsonDeserializationUtil.findConstructor(MultipleNoB2JsonConstructors.class));
        assertTrue(b2JsonException.getMessage().endsWith("has multiple constructors without @B2Json.constructor"));
    }

    @Test
    public void findConstructor_withMultipleB2JsonConstructors() {
        final B2JsonException b2JsonException = assertThrows(B2JsonException.class, () -> B2JsonDeserializationUtil.findConstructor(MultipleB2JsonConstructor.class));
        assertTrue(b2JsonException.getMessage().endsWith("has two constructors selected"));
    }

    @Test
    public void findConstructor_withNoB2JsonConstructors() {
        final B2JsonException b2JsonException = assertThrows(B2JsonException.class, () -> B2JsonDeserializationUtil.findConstructor(NoB2JsonConstructor.class));
        assertTrue(b2JsonException.getMessage().endsWith("has no constructor annotated with B2Json.constructor"));
    }

    @Test
    public void findConstructor_withInterface() {
        final B2JsonException b2JsonException = assertThrows(B2JsonException.class, () -> B2JsonDeserializationUtil.findConstructor(B2JsonInterface.class));
        assertTrue(b2JsonException.getMessage().endsWith("has no constructor"));
    }

    @Test
    public void getDiscards_noDiscards() throws B2JsonException {
        final Constructor<B2JsonConstructor> constructor = B2JsonDeserializationUtil.findConstructor(B2JsonConstructor.class);
        final B2Json.B2JsonTypeConfig b2JsonTypeConfig = new B2Json.B2JsonTypeConfig(constructor.getAnnotation(B2Json.constructor.class));
        final Set<String> discards = B2JsonDeserializationUtil.getDiscards(b2JsonTypeConfig);
        assertTrue(discards.isEmpty());
    }

    @Test
    public void getDiscards_hasDiscards() throws B2JsonException {
        final Constructor<WithDiscards> constructor = B2JsonDeserializationUtil.findConstructor(WithDiscards.class);
        final B2Json.B2JsonTypeConfig b2JsonTypeConfig = new B2Json.B2JsonTypeConfig(constructor.getAnnotation(B2Json.constructor.class));
        final Set<String> discards = B2JsonDeserializationUtil.getDiscards(b2JsonTypeConfig);
        assertEquals(new HashSet<>(Arrays.asList("extraField")), discards);
    }


    @Test
    public void getDiscards_multipleDiscards() throws B2JsonException {
        final Constructor<WithMultipleDiscards> constructor = B2JsonDeserializationUtil.findConstructor(WithMultipleDiscards.class);
        final B2Json.B2JsonTypeConfig b2JsonTypeConfig = new B2Json.B2JsonTypeConfig(constructor.getAnnotation(B2Json.constructor.class));
        final Set<String> discards = B2JsonDeserializationUtil.getDiscards(b2JsonTypeConfig);
        assertEquals(new HashSet<>(Arrays.asList("extraField1", "extraField2")), discards);
    }
    
    public interface B2JsonInterface {

    }

    public static class B2JsonConstructor {

        @B2Json.required
        public final String name;

        @B2Json.constructor(params = "name")
        public B2JsonConstructor(String name) {
            this.name = name;
        }
    }

    public static class NoB2JsonConstructor {

        @B2Json.required
        public final String name;

        public NoB2JsonConstructor(String name) {
            this.name = name;
        }
    }

    public static class MultipleB2JsonConstructor {

        @B2Json.required
        public final String name;

        @B2Json.constructor
        public MultipleB2JsonConstructor() {
            this("test");
        }

        @B2Json.constructor(params = "name")
        public MultipleB2JsonConstructor(String name) {
            this.name = name;
        }
    }

    public static class MultipleNoB2JsonConstructors {

        @B2Json.required
        public final String name;


        public MultipleNoB2JsonConstructors() {
            this("test");
        }

        public MultipleNoB2JsonConstructors(String name) {
            this.name = name;
        }
    }

    public static class WithDiscards {

        @B2Json.required
        public final String name;

        @B2Json.constructor(discards = "extraField")
        public WithDiscards(String name) {
            this.name = name;
        }
    }

    public static class WithMultipleDiscards {

        @B2Json.required
        public final String name;

        @B2Json.constructor(discards = "extraField1, extraField2")
        public WithMultipleDiscards(String name) {
            this.name = name;
        }
    }
}