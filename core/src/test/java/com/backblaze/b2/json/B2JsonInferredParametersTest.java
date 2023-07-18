/*
 * Copyright 2022, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for B2Json relying on javac's '-parameters' option instead of {@link B2Json.constructor#params}.
 */
@SuppressWarnings({
        "unused",  // A lot of the test classes have things that aren't used, but we don't care.
        "WeakerAccess"  // A lot of the test classes could have weaker access, but we don't care.
})
public class B2JsonInferredParametersTest extends B2BaseTest {

    @Rule
    public ExpectedException thrown  = ExpectedException.none();

    @Test
    public void testConstructorWithMissingFields() throws B2JsonException {
        String json = "{\"a\": 41}";
        thrown.expect(B2JsonException.class);
        thrown.expectMessage("constructor does not have the right number of parameters");
        b2Json.fromJson(json, ConstructorWithMissingFields.class);
    }

    @Test
    public void testConstructorWithExtraFields() throws B2JsonException {
        String json = "{\"a\": 41}";
        thrown.expect(B2JsonException.class);
        thrown.expectMessage("constructor does not have the right number of parameters");
        b2Json.fromJson(json, ConstructorWithExtraFields.class);
    }

    @Test
    public void testConstructorWithVersion() throws B2JsonException {
        final String json = "{}";
        final B2JsonOptions options = B2JsonOptions.builder().setVersion(1).build();
        final VersionedContainer obj = b2Json.fromJson(json, VersionedContainer.class, options);
        assertEquals(0, obj.x);
        assertEquals(1, obj.version);
    }

    @Test
    public void testDeserializeEmpty() throws B2JsonException {
        final Empty actual = B2Json.fromJsonOrThrowRuntime("{}", Empty.class);
    }

    /**
     * Deserialize a class with fields declared in different order than their corresponding
     * parameters in the constructor.
     */
    @Test
    public void testDeserializeWithMismatchingParamOrder() throws B2JsonException {
        final MismatchingOrderContainer actual = B2Json.fromJsonOrThrowRuntime(
                "{\n" +
                        "  \"a\": 41,\n" +
                        "  \"b\": \"hello\",\n" +
                        "  \"c\": 101\n" +
                        "}",
                MismatchingOrderContainer.class);
        assertEquals(41, actual.a);
        assertEquals("hello", actual.b);
        assertEquals(101, actual.c);
    }

    @Test
    public void testSeralizedFieldName() {
        String json = "{\"b\": 41}";
        final ContainerWithDifferentserializedName obj = B2Json.fromJsonOrThrowRuntime(json, ContainerWithDifferentserializedName.class);

        assertEquals(41, obj.a);
    }

    private static class Empty {
        @B2Json.constructor Empty() {}
    }

    private static class ConstructorWithMissingFields {
        @B2Json.required
        public int a;

        @B2Json.constructor ConstructorWithMissingFields() {}
    }

    private static class ConstructorWithExtraFields {
        @B2Json.required
        public int a;

        @B2Json.constructor ConstructorWithExtraFields(int a, String extraField) {}
    }

    private static class VersionedContainer {
        @B2Json.versionRange(firstVersion = 4, lastVersion = 6)
        @B2Json.required
        public final int x;

        @B2Json.ignored
        public final int version;

        @B2Json.constructor(versionParam = "v")
        public VersionedContainer(int x, int v) {
            this.x = x;
            this.version = v;
        }
    }

    private static final class MismatchingOrderContainer {

        @B2Json.required
        public final int a;

        @B2Json.required
        public final String b;

        @B2Json.required
        public int c;

        @B2Json.optional
        public final Empty d;

        @B2Json.constructor
        public MismatchingOrderContainer(int c, String b, int a, Empty d) {
            this.c = c;
            this.b = b;
            this.a = a;
            this.d = d;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MismatchingOrderContainer that = (MismatchingOrderContainer) o;

            if (a != that.a) return false;
            if (c != that.c) return false;
            if (!Objects.equals(b, that.b)) return false;
            return Objects.equals(d, that.d);
        }

        @Override
        public int hashCode() {
            int result = a;
            result = 31 * result + (b != null ? b.hashCode() : 0);
            result = 31 * result + c;
            result = 31 * result + (d != null ? d.hashCode() : 0);
            return result;
        }
    }

    private static final class Container {

        @B2Json.required
        public final int a;

        @B2Json.optional
        public final String b;

        @B2Json.ignored
        public int c;

        @B2Json.constructor
        public Container(int a, String b) {
            this.a = a;
            this.b = b;
            this.c = 5;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Container)) {
                return false;
            }
            Container other = (Container) o;
            return a == other.a && (b == null ? other.b == null : b.equals(other.b));
        }
    }

    private static class ContainerWithDifferentserializedName {
        @B2Json.required
        @B2Json.serializedName(value = "b")
        public int a;

        @B2Json.constructor
        public ContainerWithDifferentserializedName(int a) {
            this.a = a;
        }
    }

    private static final B2Json b2Json = B2Json.get();

}
