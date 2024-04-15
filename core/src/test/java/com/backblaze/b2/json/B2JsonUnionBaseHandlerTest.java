/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class B2JsonUnionBaseHandlerTest extends B2BaseTest {

    @Test
    public void testDeserialization() throws B2JsonException {
        final String json = "{\n" +
                "  \"type\": \"dog\",\n" +
                "  \"name\": \"Charlie\"\n" +
                "}";
        final Dog dog = (Dog) B2Json.get().fromJson(json, Pet.class, B2JsonOptions.DEFAULT);
        assertEquals(new Dog("Charlie"), dog);
    }

    @Test
    public void testWithExtraFieldAndAllowExtraFields() throws B2JsonException {
        final String json = "{\n" +
                "  \"type\": \"dog\",\n" +
                "  \"extraField\": \"extraValue\",\n" +
                "  \"name\": \"Charlie\"\n" +
                "}";
        final Dog dog = (Dog) B2Json.get().fromJson(json, Pet.class, B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS);
        assertEquals(new Dog("Charlie"), dog);
    }

    @Test
    public void testWithExtraFieldAndErrorOnExtraFields() {
        final String json = "{\n" +
                "  \"type\": \"dog\",\n" +
                "  \"extraField\": \"extraValue\",\n" +
                "  \"name\": \"Charlie\"\n" +
                "}";
        final B2JsonException b2JsonException = assertThrows(B2JsonException.class, () -> B2Json.get().fromJson(json, Pet.class, B2JsonOptions.DEFAULT));
        assertEquals("unknown field 'extraField' in union type Pet", b2JsonException.getMessage());
    }

    @Test
    public void testWithDiscardedField() throws B2JsonException {
        final String json = "{\n" +
                "  \"type\": \"cat\",\n" +
                "  \"breed\": \"siamese\",\n" +
                "  \"name\": \"Charlie\"\n" +
                "}";
        final Cat cat = (Cat) B2Json.get().fromJson(json, Pet.class, B2JsonOptions.DEFAULT);
        assertEquals(new Cat("Charlie"), cat);
    }

    @B2Json.union(typeField = "type")
    public static abstract class Pet {

        @SuppressWarnings("unused")
        public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
            return B2JsonUnionTypeMap
                    .builder()
                    .put("dog", Dog.class)
                    .put("cat", Cat.class)
                    .build();
        }
    }

    public static class Dog extends Pet {

        @B2Json.required
        public final String name;

        @B2Json.constructor(params = "name")
        public Dog(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Dog{" +
                    "name='" + name + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Dog)) return false;
            Dog dog = (Dog) o;
            return Objects.equals(name, dog.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    public static class Cat extends Pet {

        @B2Json.required
        public final String name;

        @B2Json.constructor(params = "name", discards = "breed")
        public Cat(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cat)) return false;
            Cat cat = (Cat) o;
            return Objects.equals(name, cat.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

}
