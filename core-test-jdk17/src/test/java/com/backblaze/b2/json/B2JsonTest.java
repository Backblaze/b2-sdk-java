/*
 * Copyright 2024, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for B2Json utilizing Java 17 features.
 */
public class B2JsonTest extends B2BaseTest {

    private static final B2Json b2Json = B2Json.get();

    @Test
    public void testRecord() throws B2JsonException {
        final String json = """
                {
                  "@d": "goodbye",
                  "a": 41,
                  "b": "hello"
                }""";
        final RecordContainer obj = new RecordContainer(41, "hello", "goodbye");
        assertEquals(json, b2Json.toJson(obj));
        assertEquals(obj, b2Json.fromJson(json, RecordContainer.class));

        final String alternateJson = """
                {
                  "a": 41,
                  "b": "hello",
                  "\\u0040d": "goodbye"
                }
                """;
        assertEquals(obj, b2Json.fromJson(alternateJson, RecordContainer.class));
    }

    @Test
    public void testRecordUnionWithTypeFieldLast() throws IOException, B2JsonException {
        final String json = """
                {
                  "a": 5,
                  "b": null,
                  "type": "a"
                }""";
        checkDeserializeSerialize(json, UnionRecordAZ.class);
    }

    @B2Json.union(typeField = "type")
    interface UnionRecordWithOutSubtypes {
    }

    @Test
    public void testRecordUnionWithNoSubtypes() {
        final String json = """
                {
                  "a": 5,
                  "b": null,
                  "type": "a"
                }""";
        final B2JsonException exception = assertThrows(B2JsonException.class, () -> b2Json.fromJson(json, UnionRecordWithOutSubtypes.class));
        assertEquals("union base class interface com.backblaze.b2.json.B2JsonTest$UnionRecordWithOutSubtypes does not have a method getUnionTypeMap", exception.getMessage());
    }

    @B2Json.union(typeField = "type")
    sealed interface UnionRecordNoSubtypes permits SubtypeRecord {
    }

    @B2Json.type
    private record SubtypeRecord(@B2Json.required int a,
                                 @B2Json.optional Set<Integer> b) implements UnionRecordNoSubtypes {
    }

    @Test
    public void testRecordUnionWithNoSubtypes_toJson() {
        final B2JsonException exception = assertThrows(B2JsonException.class, () -> b2Json.toJson(new SubtypeRecord(5, null)));
        assertEquals("interface com.backblaze.b2.json.B2JsonTest$UnionRecordNoSubtypes has B2Json.union annotation, but does not have @B2Json.unionSubtypes annotation", exception.getMessage());
    }

    @B2Json.union(typeField = "type")
    @B2Json.unionSubtypes({
            @B2Json.unionSubtypes.type(name = "a", clazz = UnionRecordWithUnknownType.definedSubtype.class)
    })
    interface UnionRecordWithUnknownType {
        @B2Json.type
        record definedSubtype(@B2Json.required int a,
                              @B2Json.optional Set<Integer> b) implements UnionRecordWithUnknownType {
        }

        @B2Json.type
        record UndefinedSubtype(@B2Json.required int a,
                                @B2Json.optional Set<Integer> b) implements UnionRecordWithUnknownType {

        }
    }

    @Test
    public void testRecordUnionWithNoMatchingSubtype_fromJson() {
        final String json = """
                {
                  "a": 5,
                  "b": null,
                  "type": "b"
                }""";
        final B2JsonException exception = assertThrows(B2JsonException.class, () -> b2Json.fromJson(json, UnionRecordWithUnknownType.class));
        assertEquals("unknown 'type' in UnionRecordWithUnknownType: 'b'", exception.getMessage());
    }

    @Test
    public void testRecordUnionWithNoMatchingSubtype_toJson() {
        final B2JsonException exception = assertThrows(B2JsonException.class, () -> b2Json.toJson(new UnionRecordWithUnknownType.UndefinedSubtype(5, null)));
        assertEquals("interface com.backblaze.b2.json.B2JsonTest$UnionRecordWithUnknownType does not contain mapping for class com.backblaze.b2.json.B2JsonTest$UnionRecordWithUnknownType$UndefinedSubtype in the @B2Json.unionSubtypes annotation", exception.getMessage());
    }

    @B2Json.union(typeField = "type")
    @B2Json.unionSubtypes({
    })
    interface UnionRecordWithEmptySubtypes {
    }

    @B2Json.type
    private record SubclassRecordWithEmptySubtypes(@B2Json.required int a,
                                                   @B2Json.optional Set<Integer> b) implements UnionRecordWithEmptySubtypes {
    }

    @Test
    public void testRecordUnionWithEmptySubtypes_fromJson() {
        final String json = """
                {
                  "a": 5,
                  "b": null,
                  "type": "b"
                }""";
        final B2JsonException exception = assertThrows(B2JsonException.class, () -> b2Json.fromJson(json, SubclassRecordWithEmptySubtypes.class));
        assertEquals("UnionRecordWithEmptySubtypes - at least one type must be configured set in @B2Json.unionSubtypes", exception.getMessage());
    }

    @Test
    public void testRecordUnionWithEmptySubtypes_toJson() {
        final B2JsonException exception = assertThrows(B2JsonException.class, () -> b2Json.toJson(new SubclassRecordWithEmptySubtypes(5, null)));
        assertEquals("UnionRecordWithEmptySubtypes - at least one type must be configured set in @B2Json.unionSubtypes", exception.getMessage());
    }

    @B2Json.union(typeField = "type")
    @B2Json.unionSubtypes({
            @B2Json.unionSubtypes.type(name = "a", clazz = SubclassRecordA.class),
            @B2Json.unionSubtypes.type(name = "z", clazz = SubclassRecordZ.class)
    })
    sealed interface UnionRecordAZ permits SubclassRecordA, SubclassRecordZ {
    }

    @B2Json.type
    private record SubclassRecordA(@B2Json.required int a, @B2Json.optional Set<Integer> b) implements UnionRecordAZ {
    }

    @B2Json.type
    private record SubclassRecordZ(@B2Json.required String z) implements UnionRecordAZ {
    }

    @Test
    public void testRecordContainingRecord() throws B2JsonException, IOException {
        final String json = """
                {
                  "age": 10,
                  "name": "Sam",
                  "record": {
                    "@d": "b",
                    "a": 5,
                    "b": "test"
                  }
                }""";
        checkDeserializeSerialize(json, RecordContainingRecord.class);
    }

    @B2Json.type
    record RecordContainingRecord(@B2Json.required int age,
                                  @B2Json.optional String name,
                                  @B2Json.optional RecordContainer record) {
    }

    record RecordContainer(@B2Json.required int a,
                           @B2Json.optional String b,
                           @B2Json.ignored int c,
                           @B2Json.optional @B2Json.serializedName(value = "@d") String d) {
        @B2Json.constructor
        RecordContainer(int a, String b, String d) {
            this(a, b, 5, d);
        }
    }

    private <T> void checkDeserializeSerialize(String json, Class<T> clazz) throws IOException, B2JsonException {
        final T obj = b2Json.fromJson(json, clazz);
        assertEquals(json, b2Json.toJson(obj));

        final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        final T obj2 = b2Json.fromJson(bytes, clazz);
        assertArrayEquals(bytes, b2Json.toJsonUtf8Bytes(obj2));

        final T obj3 = b2Json.fromJson(bytes, clazz);
        final byte[] bytesWithNewline = (json + "\n").getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(bytesWithNewline, b2Json.toJsonUtf8BytesWithNewline(obj3));
    }
}
