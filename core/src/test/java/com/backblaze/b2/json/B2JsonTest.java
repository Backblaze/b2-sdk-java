/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2Preconditions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for B2Json.
 */
@SuppressWarnings({
        "unused",  // A lot of the test classes have things that aren't used, but we don't care.
        "WeakerAccess"  // A lot of the test classes could have weaker access, but we don't care.
})
public class B2JsonTest {

    @Rule
    public ExpectedException thrown  = ExpectedException.none();

    private static final class Container {

        @B2Json.required
        public final int a;

        @B2Json.optional
        public final String b;

        @B2Json.ignored
        public int c;

        @B2Json.constructor(params = "a, b")
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

    private static final B2Json bzJson = B2Json.get();

    @Test
    public void testBoolean() throws IOException, B2JsonException {
        checkDeserializeSerialize("false", Boolean.class);
        checkDeserializeSerialize("true", Boolean.class);
    }

    @Test
    public void testBigDecimal() throws IOException, B2JsonException {
        checkDeserializeSerialize("13", BigDecimal.class);
        checkDeserializeSerialize("127.5243872835782375823758273582735", BigDecimal.class);
    }

    @Test
    public void testByte() throws IOException, B2JsonException {
        checkDeserializeSerialize("13", Byte.class);
        checkDeserializeSerialize("-37", Byte.class);
    }

    @Test
    public void testChar() throws IOException, B2JsonException {
        checkDeserializeSerialize("13", Character.class);
        checkDeserializeSerialize("65000", Character.class);
    }

    @Test
    public void testInteger() throws IOException, B2JsonException {
        checkDeserializeSerialize("-1234567", Integer.class);
    }

    @Test
    public void testLong() throws IOException, B2JsonException {
        checkDeserializeSerialize("123456789000", Long.class);
    }

    @Test
    public void testFloat() throws IOException, B2JsonException {
        checkDeserializeSerialize("3.5", Float.class);
    }

    @Test
    public void testDouble() throws IOException, B2JsonException {
        checkDeserializeSerialize("2.0E10", Double.class);
    }

    @Test
    public void testLocalDate() throws IOException, B2JsonException {
        checkDeserializeSerialize("\"20150401\"", LocalDate.class);
    }

    @Test
    public void testLocalDateTime() throws IOException, B2JsonException {
        checkDeserializeSerialize("\"d20150401_m123456\"", LocalDateTime.class);
    }

    @Test
    public void testDuration() throws IOException, B2JsonException {
        checkDeserializeSerialize("\"4d3h2m1s\"", Duration.class);
    }

    @Test
    public void testObject() throws B2JsonException {
        String json =
                "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\"\n" +
                "}";
        Container obj = new Container(41, "hello");
        assertEquals(json, bzJson.toJson(obj));
        assertEquals(obj, bzJson.fromJson(json, Container.class));
    }

    @Test
    public void testUnionWithTypeFieldLast() throws IOException, B2JsonException {
        final String json =
                "{\n" +
                        "  \"a\": 5,\n" +
                        "  \"type\": \"a\"\n" +
                        "}";
        checkDeserializeSerialize(json, UnionAZ.class);
    }

    @Test
    public void testUnionWithTypeFieldNotLast() throws IOException, B2JsonException {
        final String json =
                "{\n" +
                "  \"type\": \"z\",\n" +
                "  \"z\": \"hello\"\n" +
                "}";
        checkDeserializeSerialize(json, UnionAZ.class);
    }

    @Test
    public void testComment() throws B2JsonException {
        String json =
                "{ // this is a comment\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\"\n" +
                "} // comment to eof";
        String jsonWithoutComment =
                "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\"\n" +
                "}";
        Container obj = new Container(41, "hello");
        assertEquals(jsonWithoutComment, bzJson.toJson(obj));
        assertEquals(obj, bzJson.fromJson(json, Container.class));
    }

    @Test
    public void testNoCommentInString() throws B2JsonException {
        String json =
                "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"he//o\"\n" +
                "}";
        Container obj = new Container(41, "he//o");
        assertEquals(json, bzJson.toJson(obj));
        assertEquals(obj, bzJson.fromJson(json, Container.class));
    }

    @Test
    public void testBadComment() throws B2JsonException {
        String json =
                "{ / \n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\"\n" +
                "}";
        thrown.expect(B2JsonException.class);
        thrown.expectMessage("invalid comment: single slash");
        bzJson.fromJson(json, Container.class);
    }

    @Test
    public void testMissingComma() throws B2JsonException {
        String json =
                "{\n" +
                "  \"a\": 41\n" +
                "  \"b\": \"hello\"\n" +
                "}";
        thrown.expect(B2JsonException.class);
        thrown.expectMessage("object should end with brace but found: \"");
        bzJson.fromJson(json, Container.class);
    }

    @Test
    public void testExtraComma() throws B2JsonException {
        String json =
                "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\",\n" +
                "}";
        thrown.expect(B2JsonException.class);
        thrown.expectMessage("string does not start with quote");
        bzJson.fromJson(json, Container.class);
    }

    @Test
    public void testDuplicateField() throws B2JsonException {
        String json =
                "{\n" +
                "  \"a\": 41,\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\"\n" +
                "}";

        thrown.expect(B2JsonException.class);
        thrown.expectMessage("duplicate field: a");
        bzJson.fromJson(json, Container.class);
    }

    @Test
    public void testDisallowIgnored() throws B2JsonException {
        String json =
                "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\",\n" +
                "  \"c\": 7" +
                "}";

        thrown.expect(B2JsonException.class);
        thrown.expectMessage("unknown field");
        bzJson.fromJson(json, Container.class);
    }

    @Test
    public void testDisallowUnknown() throws B2JsonException {
        String json =
                "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\",\n" +
                "  \"x\": 7" +
                "}";

        thrown.expect(B2JsonException.class);
        thrown.expectMessage("unknown field");
        bzJson.fromJson(json, Container.class);
    }

    private static class Discarder {
        @B2Json.required
        public final int a;

        @B2Json.required
        public final int c;

        @B2Json.constructor(params = "a,c", discards = "b")
        private Discarder(int a, int c) {
            this.a = a;
            this.c = c;
        }
    }

    private static class DiscardingIgnoredFieldIsOk {
        @B2Json.required
        public final int a;

        @B2Json.ignored
        public final int c;

        @B2Json.constructor(params = "a", discards = "b,c")
        private DiscardingIgnoredFieldIsOk(int a) {
            this.a = a;
            this.c = 42;
        }
    }


    private static class DiscardingNonIgnoredFieldIsIllegal {
        @B2Json.required
        public final int a;

        @B2Json.required
        public final int c;

        @B2Json.constructor(params = "a,c", discards = "b,c")
        private DiscardingNonIgnoredFieldIsIllegal(int a, int c) {
            this.a = a;
            this.c = c;
        }
    }

    @Test
    public void testAllowUnknown() throws B2JsonException {
        String json =
                "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\",\n" +
                "  \"x\": 7" +
                "}";

        Container c = bzJson.fromJson(json, Container.class, B2Json.ALLOW_EXTRA_FIELDS);

        String expectedJson =
                "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\"\n" +
                "}";
        assertEquals(expectedJson, bzJson.toJson(c));
    }

    @Test
    public void testAllowButSkipDiscarded() throws B2JsonException {
        final String jsonWithExtra = "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\",\n" +
                "  \"c\": 7" +
                "}";

        final Discarder discarder = bzJson.fromJson(jsonWithExtra, Discarder.class);
        assertEquals(41, discarder.a);
        assertEquals(7, discarder.c);

        final String expectedJson = "{\n" +
                "  \"a\": 41,\n" +
                "  \"c\": 7\n" +
                "}";
        assertEquals(expectedJson, bzJson.toJson(discarder));
    }

    @Test
    public void testDiscardingIgnoredFieldIsOk() throws B2JsonException {
        final String jsonWithExtra = "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\",\n" +
                "  \"c\": 7" +
                "}";

        final DiscardingIgnoredFieldIsOk discarder = bzJson.fromJson(jsonWithExtra, DiscardingIgnoredFieldIsOk.class);
        assertEquals(41, discarder.a);
        assertEquals(42, discarder.c); // 'cuz ignored from json and set by constructor.

        final String expectedJson = "{\n" +
                "  \"a\": 41\n" +
                "}";
        assertEquals(expectedJson, bzJson.toJson(discarder));
    }

    @Test
    public void testDiscardingNonIgnoredFieldIsIllegal() throws B2JsonException {
        thrown.expect(B2JsonException.class);
        thrown.expectMessage("DiscardingNonIgnoredFieldIsIllegal's field 'c' cannot be discarded: it's REQUIRED.  only non-existent or IGNORED fields can be discarded.");
        final String jsonWithExtra = "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\",\n" +
                "  \"c\": 7" +
                "}";

        bzJson.fromJson(jsonWithExtra, DiscardingNonIgnoredFieldIsIllegal.class);
    }


    @Test
    public void testMissingRequired() throws B2JsonException {
        String json = "{ \"b\" : \"hello\" }";

        thrown.expect(B2JsonException.class);
        thrown.expectMessage("required field a is missing");
        bzJson.fromJson(json, Container.class);
    }

    private static class Empty {
        @B2Json.constructor(params = "") Empty() {}
    }

    private static class AllOptionalTypes {
        @B2Json.optional boolean v_boolean;
        @B2Json.optional byte v_byte;
        @B2Json.optional int v_int;
        @B2Json.optional char v_char;
        @B2Json.optional long v_long;
        @B2Json.optional float v_float;
        @B2Json.optional double v_double;
        @B2Json.optional String v_string;
        @B2Json.optional Empty v_empty;
        @B2Json.optional Color v_color;

        @B2Json.constructor(params = "v_boolean, v_byte, v_char, v_int, v_long, v_float, v_double, v_string, v_empty, v_color")
        public AllOptionalTypes(boolean v_boolean,
                                byte v_byte,
                                char v_char,
                                int v_int,
                                long v_long,
                                float v_float,
                                double v_double,
                                String v_string,
                                Empty v_empty,
                                Color v_color) {
            this.v_boolean = v_boolean;
            this.v_byte = v_byte;
            this.v_int = v_int;
            this.v_char = v_char;
            this.v_long = v_long;
            this.v_float = v_float;
            this.v_double = v_double;
            this.v_string = v_string;
            this.v_empty = v_empty;
            this.v_color = v_color;
        }
    }

    @Test
    public void testOptionalNotPresent() throws IOException, B2JsonException {
        String json = "{}";
        AllOptionalTypes obj = bzJson.fromJson(json, AllOptionalTypes.class);
        assertFalse(obj.v_boolean);
        assertEquals(0, obj.v_byte);
        assertEquals(0, obj.v_char);
        assertEquals(0, obj.v_int);
        assertEquals(0, obj.v_long);
        assertEquals(0.0, obj.v_float, 0.0);
        assertEquals(0.0, obj.v_double, 0.0);
        assertNull(obj.v_string);
        assertNull(obj.v_empty);

        String expectedJson =
                "{\n" +
                "  \"v_boolean\": false,\n" +
                "  \"v_byte\": 0,\n" +
                "  \"v_char\": 0,\n" +
                "  \"v_color\": null,\n" +
                "  \"v_double\": 0.0,\n" +
                "  \"v_empty\": null,\n" +
                "  \"v_float\": 0.0,\n" +
                "  \"v_int\": 0,\n" +
                "  \"v_long\": 0,\n" +
                "  \"v_string\": null\n" +
                "}";
        assertEquals(expectedJson, bzJson.toJson(obj));

        checkDeserializeSerialize(expectedJson, AllOptionalTypes.class);
    }

    @Test
    public void testOptionalsPresentInUrl() throws IOException, B2JsonException {
        Map<String, String> parameterMap = makeParameterMap(
                "v_boolean", "true",
                "v_byte", "5",
                "v_char", "6",
                "v_color", "BLUE",
                "v_double", "7.0",
                "v_float", "8.0",
                "v_int", "9",
                "v_long", "10",
                "v_string", "abc"
        );
        AllOptionalTypes obj = bzJson.fromUrlParameterMap(parameterMap, AllOptionalTypes.class);
        assertTrue(obj.v_boolean);
        assertEquals(5, obj.v_byte);
        assertEquals(6, obj.v_char);
        assertEquals(Color.BLUE, obj.v_color);
        assertEquals(7.0, obj.v_double, 0.0001);
        assertEquals(8.0, obj.v_float, 0.0001);
        assertEquals(9, obj.v_int);
        assertEquals(10, obj.v_long);
        assertEquals("abc", obj.v_string);
    }



    private Map<String, String> makeParameterMap(String ... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("odd number of arguments");
        }
        Map<String, String> parameterMap = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            parameterMap.put(args[i], args[i + 1]);
        }
        return parameterMap;
    }



    @Test
    public void testOptionalNull()throws B2JsonException {
        String json =
                "{" +
                "  \"v_string\" : null,\n" +
                "  \"v_empty\" : null\n" +
                "}";
        AllOptionalTypes obj = bzJson.fromJson(json, AllOptionalTypes.class);
        assertNull(obj.v_string);
        assertNull(obj.v_empty);
    }

    private static class RequiredObject {
        @B2Json.required Empty a;

        @B2Json.constructor(params = "a")
        public RequiredObject(Empty a) {
            this.a = a;
        }
    }

    @Test
    public void testSerializeNullTopLevel() throws B2JsonException {
        thrown.expect(B2JsonException.class);
        thrown.expectMessage("top level object must not be null");
        bzJson.toJson(null);
    }

    @Test
    public void testSerializeNullRequired() throws B2JsonException {
        RequiredObject obj = new RequiredObject(null);

        thrown.expect(B2JsonException.class);
        thrown.expectMessage("required field a cannot be null");
        bzJson.toJson(obj);
    }

    @Test
    public void testDeserializeNullRequired() throws B2JsonException {
        String json = "{ \"a\" : null }";

        thrown.expect(B2JsonException.class);
        thrown.expectMessage("required field a cannot be null");
        bzJson.fromJson(json, RequiredObject.class);
    }

    private static class ListHolder {
        @B2Json.optional
        List<List<Integer>> intListList;

        @B2Json.constructor(params = "intListList")
        ListHolder(List<List<Integer>> intListList) {
            this.intListList = intListList;
        }
    }

    @Test
    public void testList() throws IOException, B2JsonException {
        String json1 =
                "{\n" +
                "  \"intListList\": [\n" +
                "    [ 1, null, 3 ]\n" +
                "  ]\n" +
                "}";
        checkDeserializeSerialize(json1, ListHolder.class);

        String json2 =
                "{\n" +
                "  \"intListList\": null\n" +
                "}";
        checkDeserializeSerialize(json2, ListHolder.class);
    }

    private <T> void checkDeserializeSerialize(String json, Class<T> clazz) throws IOException, B2JsonException {
        T obj = bzJson.fromJson(json, clazz);
        assertEquals(json, bzJson.toJson(obj));

        byte [] bytes = getUtf8Bytes(json);
        T obj2 = bzJson.fromJson(bytes, clazz);
        assertArrayEquals(bytes, bzJson.toJsonUtf8Bytes(obj2));

        T obj3 = bzJson.fromJson(bytes, clazz);
        byte [] bytesWithNewline = getUtf8Bytes(json + "\n");
        assertArrayEquals(bytesWithNewline, bzJson.toJsonUtf8BytesWithNewline(obj3));
    }

    private <T> void checkDeserializeSerialize(String json, Class<T> clazz, String expectedJson) throws IOException, B2JsonException {
        T obj = bzJson.fromJson(json, clazz);
        assertEquals(expectedJson, bzJson.toJson(obj));

        byte [] bytes = getUtf8Bytes(json);
        T obj2 = bzJson.fromJson(bytes, clazz);
        assertArrayEquals(getUtf8Bytes(expectedJson), bzJson.toJsonUtf8Bytes(obj2));
    }

    private static class MapHolder {
        @B2Json.optional
        public Map<LocalDate, Integer> map;

        @B2Json.constructor(params = "map")
        public MapHolder(Map<LocalDate, Integer> map) {
            this.map = map;
        }
    }

    @Test
    public void testMap() throws IOException, B2JsonException {
        String json1 =
                "{\n" +
                "  \"map\": {\n" +
                "    \"20150101\": 37,\n" +
                "    \"20150207\": null\n" +
                "  }\n" +
                "}" ;
        checkDeserializeSerialize(json1, MapHolder.class);

        String json2 =
                "{\n" +
                "  \"map\": null\n" +
                "}";
        checkDeserializeSerialize(json2, MapHolder.class);
    }

    private static class MapWithNullKeyHolder {
        @B2Json.optional
        Map<String, String> map;

        @B2Json.constructor(params = "map")
        public MapWithNullKeyHolder(Map<String, String> map) {
            this.map = map;
        }
    }
    @Test
    public void testSerializationOfMapWithNullKeyGeneratesException() {
        Map<String, String> map = new HashMap<>();
        map.put(null, "Text");
        MapWithNullKeyHolder mapWithNullKeyHolder = new MapWithNullKeyHolder(map);
        try {
            bzJson.toJson(mapWithNullKeyHolder);
            assertTrue("Map with null key should not be allowed to be serialized", false);
        } catch (B2JsonException ex) {
            assertEquals("Map key is null", ex.getMessage());
        }
    }

    private static class TreeMapHolder {
        @B2Json.optional
        TreeMap<LocalDate, Integer> treeMap;

        @B2Json.constructor(params = "treeMap")
        public TreeMapHolder(TreeMap<LocalDate, Integer> treeMap) {
            this.treeMap = treeMap;
        }
    }

    @Test
    public void testTreeMap() throws IOException, B2JsonException {
        String json1 =
                "{\n" +
                        "  \"treeMap\": {\n" +
                        "    \"20150101\": 37,\n" +
                        "    \"20150207\": null\n" +
                        "  }\n" +
                        "}" ;
        checkDeserializeSerialize(json1, TreeMapHolder.class);

        String json2 =
                "{\n" +
                        "  \"treeMap\": null\n" +
                        "}";
        checkDeserializeSerialize(json2, TreeMapHolder.class);
    }


    private static class ConcurrentMapHolder {
        @B2Json.optional
        public ConcurrentMap<LocalDate, Integer> map;

        @B2Json.constructor(params = "map")
        public ConcurrentMapHolder(ConcurrentMap<LocalDate, Integer> map) {
            this.map = map;
        }
    }

    @Test
    public void testConcurrentMap() throws B2JsonException {
        ConcurrentMap<LocalDate, Integer> map = new ConcurrentHashMap<>();
        map.put(LocalDate.of(2015, 5, 2), 7);
        ConcurrentMapHolder holder = new ConcurrentMapHolder(map);
        String json =
                "{\n" +
                "  \"map\": {\n" +
                "    \"20150502\": 7\n" +
                "  }\n" +
                "}" ;
        assertEquals(json, bzJson.toJson(holder));
    }

    @Test
    public void testDirectMap() throws B2JsonException {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 5);
        map.put("b", 6);
        String json =
                "{\n" +
                "  \"a\": 5,\n" +
                "  \"b\": 6\n" +
                "}";
        assertEquals(json, bzJson.mapToJson(map, String.class, Integer.class));
        assertEquals(map, bzJson.mapFromJson(json, String.class, Integer.class));
    }

    @Test
    public void testDirectList() throws B2JsonException {
        List<String> list = new ArrayList<>();
        list.add("alfa");
        list.add("bravo");
        String json =
                "[\n" +
                "  \"alfa\",\n" +
                "  \"bravo\"\n" +
                "]";
        assertEquals(json, bzJson.listToJson(list, String.class));
        assertEquals(list, bzJson.listFromJson(json, String.class));
    }


    @Test
    public void testUtf8() throws IOException, B2JsonException {
        // These test cases are from: http://www.oracle.com/us/technologies/java/supplementary-142654.html
        StringBuilder builder = new StringBuilder();
        builder.appendCodePoint(0xA);
        builder.appendCodePoint(0x41);
        builder.appendCodePoint(0xDF);
        builder.appendCodePoint(0x6771);
        builder.appendCodePoint(0x10400);
        String str = builder.toString();

        String json = "\"\\u000a" + str.substring(1) + "\"";

        byte [] utf8Json = new byte [] {
                (byte) 0x22,
                (byte) 0x5C,
                (byte) 0x75,
                (byte) 0x30,
                (byte) 0x30,
                (byte) 0x30,
                (byte) 0x61,
                (byte) 0x41,
                (byte) 0xC3,
                (byte) 0x9F,
                (byte) 0xE6,
                (byte) 0x9D,
                (byte) 0xB1,
                (byte) 0xF0,
                (byte) 0x90,
                (byte) 0x90,
                (byte) 0x80,
                (byte) 0x22
        };

        // Check de-serializing from string
        assertEquals(str, bzJson.fromJson(json, String.class));

        // Check de-serializing from bytes
        assertEquals(str, bzJson.fromJson(new ByteArrayInputStream(utf8Json), String.class));

        // Check serializing to string
        assertEquals(json, bzJson.toJson(str));

        // Check serializing to bytes
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            bzJson.toJson(str, out);
            assertArrayEquals(utf8Json, out.toByteArray());
        }
    }


    private static class Private {
        @B2Json.required
        private int age;

        @B2Json.constructor(params = "age")
        private Private(int age) {
            this.age = age;
        }
    }

    @Test
    public void testPrivate() throws B2JsonException, IOException {
        final Private orig = new Private(6);
        assertEquals(6, orig.age);

        // ensure we can serialize a private member.
        final String json = bzJson.toJson(orig);
        final String expectedJson =
                "{\n" +
                "  \"age\": 6\n" +
                "}";
        assertEquals(expectedJson, json);

        // ensure we can create with a private constructor and set a private member.
        checkDeserializeSerialize(json, Private.class);
    }

    // an enum with no @B2Json.defaultForInvalidValue
    private enum Color { BLUE, GREEN, RED }

    private static class ColorHolder {
        @B2Json.required
        Color color;

        @B2Json.constructor(params =  "color")
        public ColorHolder(Color color) {
            this.color = color;
        }
    }

    @Test
    public void testEnum() throws IOException, B2JsonException {
        String json =
                "{\n" +
                "  \"color\": \"RED\"\n" +
                "}";
        checkDeserializeSerialize(json, ColorHolder.class);
    }

    @Test
    public void testUnknownEnum_noDefaultForInvalidEnumValue() throws IOException, B2JsonException {
        String json =
                "{\n" +
                "  \"color\": \"CHARTREUSE\"\n" +
                "}";

        thrown.expect(B2JsonException.class);
        thrown.expectMessage("CHARTREUSE is not a valid value.  Valid values are: BLUE, GREEN, RED");
        checkDeserializeSerialize(json, ColorHolder.class);
    }

    // an enum with too many @B2Json.defaultForInvalidValues
    private enum Spin {
        @B2Json.defaultForInvalidEnumValue()
        LEFT,

        @B2Json.defaultForInvalidEnumValue()
        RIGHT
    }

    private static class SpinHolder {
        @B2Json.optional
        final Spin spin;

        @B2Json.constructor(params = "spin")
        private SpinHolder(Spin spin) {
            this.spin = spin;
        }
    }

    @Test
    public void testUnknownEnum_tooManyDefaultInvalidEnumValue() throws IOException, B2JsonException {
        String json =
                "{\n" +
                "  \"spin\": \"CHARTREUSE\"\n" +
                "}";

        thrown.expect(B2JsonException.class);
        thrown.expectMessage("more than one @B2Json.defaultForInvalidEnumValue annotation in enum class com.backblaze.b2.json.B2JsonTest.Spin");
        checkDeserializeSerialize(json, SpinHolder.class);
    }


    // an enum with one @B2Json.defaultForInvalidEnumValue
    private enum Flavor {
        UP,
        DOWN,

        @B2Json.defaultForInvalidEnumValue
        STRANGE,
        CHARM,
        TOP,
        BOTTOM
    }

    private static class FlavorHolder {
        @B2Json.optional
        final Flavor flavor;

        @B2Json.constructor(params = "flavor")
        private FlavorHolder(Flavor flavor) {
            this.flavor = flavor;
        }
    }


    @Test
    public void testUnknownEnum_usesDefaultInvalidEnumValue() throws B2JsonException {
        String json =
                "{\n" +
                        "  \"flavor\": \"CHARTREUSE\"\n" +
                        "}";

        final FlavorHolder holder = B2Json.get().fromJson(json, FlavorHolder.class);
        assertEquals(Flavor.STRANGE, holder.flavor);
    }

    private static class EvenNumber {

        @SuppressWarnings("unused")
        @B2Json.required
        private final int number;

        @B2Json.constructor(params =  "number")
        public EvenNumber(int number) {
            if (number % 2 != 0) {
                throw new IllegalArgumentException("not even: " + number);
            }
            this.number = number;
        }
    }

    @Test
    public void testConstructorThrowsIllegalArgument() throws B2JsonException {
        String json = "{ \"number\" : 7 }";
        thrown.expect(B2JsonBadValueException.class);
        thrown.expectMessage("not even: 7");
        bzJson.fromJson(json, EvenNumber.class);
    }

    private static class PrimitiveArrayContainer {
        @B2Json.required
        final boolean[] booleans;

        @B2Json.required
        final char[] chars;

        @B2Json.required
        final byte[] bytes;

        @B2Json.required
        final int[] ints;

        @B2Json.required
        final long[] longs;

        @B2Json.required
        final float[] floats;

        @B2Json.required
        final double[] doubles;

        @B2Json.constructor(params = "booleans,chars,bytes,ints,longs,floats,doubles")
        public PrimitiveArrayContainer(boolean[] booleans,
                                       char[] chars,
                                       byte[] bytes,
                                       int[] ints,
                                       long[] longs,
                                       float[] floats,
                                       double[] doubles) {
            this.booleans = booleans;
            this.chars = chars;
            this.bytes = bytes;
            this.ints = ints;
            this.longs = longs;
            this.floats = floats;
            this.doubles = doubles;
        }
    }

    private static class OptionalPrimitiveArrayContainer {
        @B2Json.optional
        final boolean[] booleans;

        @B2Json.optional
        final char[] chars;

        @B2Json.optional
        final byte[] bytes;

        @B2Json.optional
        final int[] ints;

        @B2Json.optional
        final long[] longs;

        @B2Json.optional
        final float[] floats;

        @B2Json.optional
        final double[] doubles;

        @B2Json.constructor(params = "booleans,chars,bytes,ints,longs,floats,doubles")
        public OptionalPrimitiveArrayContainer(boolean[] booleans,
                                               char[] chars,
                                               byte[] bytes,
                                               int[] ints,
                                               long[] longs,
                                               float[] floats,
                                               double[] doubles) {
            this.booleans = booleans;
            this.chars = chars;
            this.bytes = bytes;
            this.ints = ints;
            this.longs = longs;
            this.floats = floats;
            this.doubles = doubles;
        }
    }

    @Test
    public void testEmptyArrays() throws B2JsonException, IOException {
        final String json = "{\n" +
                "  \"booleans\": [],\n" +
                "  \"bytes\": [],\n" +
                "  \"chars\": [],\n" +
                "  \"doubles\": [],\n" +
                "  \"floats\": [],\n" +
                "  \"ints\": [],\n" +
                "  \"longs\": []\n" +
                "}";
        checkDeserializeSerialize(json, PrimitiveArrayContainer.class);
    }

    @Test
    public void testArraysWithValues() throws B2JsonException, IOException {
        final String json = "{\n" +
                "  \"booleans\": [ true, false ],\n" +
                "  \"bytes\": [ 1, 2, 3 ],\n" +
                "  \"chars\": [ 65, 0, 128, 255 ],\n" +
                "  \"doubles\": [ 1.1, -2.2 ],\n" +
                "  \"floats\": [ 1.0, 2.0, 3.0 ],\n" +
                "  \"ints\": [ -2147483648, 0, 2147483647 ],\n" +
                "  \"longs\": [ 9223372036854775807, -9223372036854775808 ]\n" +
                "}";
        checkDeserializeSerialize(json, PrimitiveArrayContainer.class);
    }

    @Test
    public void testOptionalArraysWithValues() throws B2JsonException, IOException {
        final String json = "{\n" +
                "  \"booleans\": [ true ],\n" +
                "  \"bytes\": [ 1 ],\n" +
                "  \"chars\": [ 65 ],\n" +
                "  \"doubles\": [ 1.0 ],\n" +
                "  \"floats\": [ 2.0 ],\n" +
                "  \"ints\": [ 257 ],\n" +
                "  \"longs\": [ 12345 ]\n" +
                "}";
        checkDeserializeSerialize(json, OptionalPrimitiveArrayContainer.class);
    }

    @Test
    public void testOptionalArraysWithMissing() throws B2JsonException, IOException {
        final String json = "{}";
        final String expectedJson = "{\n" +
                "  \"booleans\": null,\n" +
                "  \"bytes\": null,\n" +
                "  \"chars\": null,\n" +
                "  \"doubles\": null,\n" +
                "  \"floats\": null,\n" +
                "  \"ints\": null,\n" +
                "  \"longs\": null\n" +
                "}";
        checkDeserializeSerialize(json, OptionalPrimitiveArrayContainer.class, expectedJson);
    }

    @Test
    public void testOptionalArraysThatAreNulls() throws B2JsonException, IOException {
        final String json = "{\n" +
                "  \"booleans\": null,\n" +
                "  \"bytes\": null,\n" +
                "  \"chars\": null,\n" +
                "  \"doubles\": null,\n" +
                "  \"floats\": null,\n" +
                "  \"ints\": null,\n" +
                "  \"longs\": null\n" +
                "}";
        checkDeserializeSerialize(json, OptionalPrimitiveArrayContainer.class);
    }

    private void checkNullInArray(String fieldType) throws IOException {
        final String json = "{\n" +
                "  \"" + fieldType + "s\": [ null ]\n" +
                "}";
        try {
            checkDeserializeSerialize(json, OptionalPrimitiveArrayContainer.class);
            fail("should've thrown");
        } catch (B2JsonException e) {
            assertEquals("can't put null in a " + fieldType + "[].", e.getMessage());
        }
    }

    @Test
    public void testNullInPrimitiveArray() throws IOException {
        checkNullInArray("boolean");
        checkNullInArray("byte");
        checkNullInArray("char");
        checkNullInArray("double");
        checkNullInArray("float");
        checkNullInArray("int");
        checkNullInArray("boolean");
        checkNullInArray("long");
    }

    private static class ObjectArrayContainer {
        @B2Json.required
        final OptionalPrimitiveArrayContainer[] containers;

        @B2Json.required
        final Color[] colors;

        @B2Json.required
        final long[][] arrays;

        @B2Json.constructor(params = "containers, colors, arrays")
        public ObjectArrayContainer(OptionalPrimitiveArrayContainer[] containers,
                                    Color[] colors,
                                    long[][] arrays) {
            this.containers = containers;
            this.colors = colors;
            this.arrays = arrays;
        }
    }

    @Test
    public void testEmptyObjectArrays() throws IOException, B2JsonException {
        final String json = "{\n" +
                "  \"arrays\": [],\n" +
                "  \"colors\": [],\n" +
                "  \"containers\": []\n" +
                "}";
        checkDeserializeSerialize(json, ObjectArrayContainer.class);
    }

    @Test
    public void testObjectArrays() throws IOException, B2JsonException {
        final String json = "{\n" +
                "  \"arrays\": [\n" +
                "    [ 1, 2, 3 ],\n" +
                "    null,\n" +
                "    [ 4, 5, 6 ]\n" +
                "  ],\n" +
                "  \"colors\": [\n" +
                "    \"RED\",\n" +
                "    null,\n" +
                "    \"BLUE\"\n" +
                "  ],\n" +
                "  \"containers\": [\n" +
                "    {\n" +
                "      \"booleans\": [ true ],\n" +
                "      \"bytes\": null,\n" +
                "      \"chars\": null,\n" +
                "      \"doubles\": null,\n" +
                "      \"floats\": null,\n" +
                "      \"ints\": null,\n" +
                "      \"longs\": null\n" +
                "    },\n" +
                "    null,\n" +
                "    {\n" +
                "      \"booleans\": null,\n" +
                "      \"bytes\": null,\n" +
                "      \"chars\": null,\n" +
                "      \"doubles\": null,\n" +
                "      \"floats\": null,\n" +
                "      \"ints\": null,\n" +
                "      \"longs\": [ 7, 8, 9 ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        checkDeserializeSerialize(json, ObjectArrayContainer.class);
    }

    private byte [] getUtf8Bytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No UTF-8 charset");
        }
    }

    private static class GoodCustomHandler {
        @SuppressWarnings("unused") // used by reflection
        private static B2JsonTypeHandler<GoodCustomHandler> getJsonTypeHandler() {
            return new JsonHandler();
        }

        private static class JsonHandler implements B2JsonTypeHandler<GoodCustomHandler> {
            public Class<GoodCustomHandler> getHandledClass() {
                return GoodCustomHandler.class;
            }

            public void serialize(GoodCustomHandler obj, B2JsonWriter out) throws IOException {
                out.writeString("GoodCustomHandler");
            }

            public GoodCustomHandler deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
                return deserialize(in.readString());
            }

            public GoodCustomHandler deserializeUrlParam(String urlValue) throws B2JsonException {
                return deserialize(urlValue);
            }

            private GoodCustomHandler deserialize(String value) throws B2JsonBadValueException {
                if (!value.equals("GoodCustomHandler")) {
                    throw new B2JsonBadValueException("string isn't a GoodCustomHandler (" + value + ")");
                }
                return new GoodCustomHandler();
            }

            public GoodCustomHandler defaultValueForOptional() {
                return null;
            }

            public boolean isStringInJson() {
                return true;
            }
        }
    }

    @Test
    public void customHandlerGood() throws IOException, B2JsonException {
        checkDeserializeSerialize("\"GoodCustomHandler\"", GoodCustomHandler.class);
    }


    private static class WrongTypeHandler {
        @SuppressWarnings("unused")
        private static Double getJsonTypeHandler() {
            return 6.66;
        }
    }

    @Test
    public void customHandlerWrongType() throws IOException {
        try {
            checkDeserializeSerialize("{}", WrongTypeHandler.class);
            fail("should've thrown!");
        } catch (B2JsonException e) {
            assertEquals("WrongTypeHandler.getJsonTypeHandler() returned an unexpected type of object (java.lang.Double)", e.getMessage());
        }
    }


    private static class NullHandler {
        @SuppressWarnings("unused") // used by reflection
        private static B2JsonTypeHandler<NullHandler> getJsonTypeHandler() {
            return null;
        }
    }

    @Test
    public void customHandlerNull() throws IOException {
        try {
            checkDeserializeSerialize("{}", NullHandler.class);
            fail("should've thrown!");
        } catch (B2JsonException e) {
            assertEquals("NullHandler.getJsonTypeHandler() returned an unexpected type of object (null)", e.getMessage());
        }
    }


    private static class OptionalWithDefaultHolder {

        @B2Json.optional
        public final int a;

        @B2Json.optionalWithDefault(defaultValue = "5")
        public final int b;

        @B2Json.optionalWithDefault(defaultValue = "\"hello\"")
        public final String c;

        @B2Json.constructor(params = "a, b, c")
        private OptionalWithDefaultHolder(int a, int b, String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OptionalWithDefaultHolder that = (OptionalWithDefaultHolder) o;
            return Objects.equals(a, that.a) &&
                    Objects.equals(b, that.b) &&
                    Objects.equals(c, that.c);
        }

        @Override
        public String toString() {
            return "OptionalWithDefaultHolder{" +
                    "a=" + a +
                    ", b=" + b +
                    ", c='" + c + '\'' +
                    '}';
        }
    }

    @Test
    public void testOptionalWithDefault() throws B2JsonException {
        {
            OptionalWithDefaultHolder expected = new OptionalWithDefaultHolder(0, 5, "hello");
            OptionalWithDefaultHolder actual = bzJson.fromJson("{}", OptionalWithDefaultHolder.class);
            assertEquals(expected, actual);
        }
        {
            OptionalWithDefaultHolder expected = new OptionalWithDefaultHolder(2, 3, "4");
            OptionalWithDefaultHolder actual = bzJson.fromJson("{\"a\": 2, \"b\": 3, \"c\": \"4\"}", OptionalWithDefaultHolder.class);
            assertEquals(expected, actual);
        }
    }

    private static final class Node {
        @B2Json.optional
        public String name;

        @B2Json.optional
        List<Node> childNodes;

        @B2Json.constructor(params = "name, childNodes")
        public Node(String name, List<Node> childNodes) {
            this.name = name;
            this.childNodes = childNodes;
        }
    }

    @Test
    public void testRecursiveTree() throws IOException, B2JsonException {

        String tree1 =
            "{\n" +
            "  \"childNodes\": null,\n" +
            "  \"name\": \"Degenerative\"\n" +
            "}";
        checkDeserializeSerialize(tree1, Node.class);

        String tree2 =
            "{\n" +
            "  \"childNodes\": [],\n" +
            "  \"name\": \"With Empty Children\"\n" +
            "}";
        checkDeserializeSerialize(tree2, Node.class);

        String tree3 =
            "{\n" +
            "  \"childNodes\": [\n" +
            "    {\n" +
            "      \"childNodes\": null,\n" +
            "      \"name\": \"Level 2 Child 1\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"childNodes\": [\n" +
            "        {\n" +
            "          \"childNodes\": null,\n" +
            "          \"name\": \"Level 3 Child 1\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"name\": \"Level 2 Child 2\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"name\": \"Top\"\n" +
            "}";
        checkDeserializeSerialize(tree3, Node.class);
    }

    @Test
    public void testEnumWithSpecialHandler() throws B2JsonException {
        String json = "{\"letter\": \"b\"}";
        LetterHolder holder = B2Json.get().fromJson(json, LetterHolder.class);
        assertEquals(Letter.BEE, holder.letter);
    }

    private static class LetterHolder {
        @B2Json.required
        private final Letter letter;

        @B2Json.constructor(params = "letter")
        private LetterHolder(Letter letter) {
            this.letter = letter;
        }
    }
    private enum Letter {
        BEE;

        public static class JsonHandler implements B2JsonTypeHandler<Letter> {
            @Override
            public Class<Letter> getHandledClass() {
                return Letter.class;
            }

            @Override
            public void serialize(Letter obj, B2JsonWriter out) throws IOException {
                out.writeString("b");
            }

            @Override
            public Letter deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
                return deserializeUrlParam(in.readString());
            }

            @Override
            public Letter deserializeUrlParam(String urlValue) {
                B2Preconditions.checkArgument(urlValue.equals("b"));
                return BEE;
            }

            @Override
            public Letter defaultValueForOptional() {
                return null;
            }

            @Override
            public boolean isStringInJson() {
                return true;
            }
        }

        @SuppressWarnings("unused") // used by reflection
        private static B2JsonTypeHandler<Letter> getJsonTypeHandler() {
            return new Letter.JsonHandler();
        }
    }

    @Test
    public void testSerializeUnion() throws B2JsonException {
        thrown.expectMessage("is a union base class, and cannot be serialized");
        B2Json.get().toJson(new UnionAZ());
    }

    @Test
    public void testFieldFromWrongTypeInUnion() throws B2JsonException {
        final String json = "{ \"z\" : \"hello\", \"type\" : \"a\" }";
        thrown.expectMessage("unknown field in com.backblaze.b2.json.B2JsonTest$SubclassA: z");
        B2Json.get().fromJson(json, UnionAZ.class);
    }

    @Test
    public void testMissingTypeInUnion() throws B2JsonException {
        final String json = "{ \"a\" : 5 }";
        thrown.expectMessage("missing 'type' in UnionAZ");
        B2Json.get().fromJson(json, UnionAZ.class);
    }

    @Test
    public void testUnknownTypeInUnion() throws B2JsonException {
        final String json = "{ \"type\" : \"bad\" }";
        thrown.expectMessage("unknown 'type' in UnionAZ: 'bad'");
        B2Json.get().fromJson(json, UnionAZ.class);
    }

    @Test
    public void testUnknownFieldInUnion() throws B2JsonException {
        final String json = "{ \"badField\" : 5 }";
        thrown.expectMessage("unknown field 'badField' in union type UnionAZ");
        B2Json.get().fromJson(json, UnionAZ.class);
    }

    @B2Json.union(typeField = "type")
    private static class UnionAZ {
        public static Map<String, Class<?>> getUnionTypeMap() {
            Map<String, Class<?>> result = new HashMap<>();
            result.put("a", SubclassA.class);
            result.put("z", SubclassZ.class);
            return result;
        }
    }

    private static class SubclassA extends UnionAZ {
        @B2Json.required
        public final int a;

        @B2Json.constructor(params = "a")
        private SubclassA(int a) {
            this.a = a;
        }
    }

    private static class SubclassZ extends UnionAZ {
        @B2Json.required
        public final String z;

        @B2Json.constructor(params = "z")
        private SubclassZ(String z) {
            this.z = z;
        }
    }

    @Test
    public void testUnionWithFieldAnnotation() throws B2JsonException {
        thrown.expectMessage("field annotations not allowed in union class");
        B2Json.get().fromJson("{}", BadUnionWithFieldAnnotation.class);
    }

    @B2Json.union(typeField = "foo")
    private static class BadUnionWithFieldAnnotation {
        @B2Json.required
        public int x;
    }

    @Test
    public void testUnionWithConstructorAnnotation() throws B2JsonException {
        thrown.expectMessage("constructor annotations not allowed in union class");
        B2Json.get().fromJson("{}", BadUnionWithConstructorAnnotation.class);
    }

    @B2Json.union(typeField = "foo")
    private static class BadUnionWithConstructorAnnotation {
        @B2Json.constructor(params = "")
        public BadUnionWithConstructorAnnotation() {}
    }

    @Test
    public void testUnionWithoutGetMap() throws B2JsonException {
        thrown.expectMessage("does not have a method getUnionTypeMap");
        B2Json.get().fromJson("{}", UnionWithoutGetMap.class);
    }

    @B2Json.union(typeField = "type")
    private static class UnionWithoutGetMap {}

    @Test
    public void testUnionTypeMapNotAMap() throws B2JsonException {
        thrown.expectMessage("UnionWithNonMap.getUnionTypeMap() did not return a Map");
        B2Json.get().fromJson("{}", UnionWithNonMap.class);
    }

    @B2Json.union(typeField = "type")
    private static class UnionWithNonMap {
        public static String getUnionTypeMap() {
            return "foo";
        }
    }

    @Test
    public void testBadKeyTypeInUnionTypeMap() throws B2JsonException {
        thrown.expectMessage("returned a map containing a class java.lang.Integer as a key");
        B2Json.get().fromJson("{}", UnionWithBadKeyInMap.class);
    }

    @B2Json.union(typeField = "type")
    private static class UnionWithBadKeyInMap {
        public static Map<Integer, Class<?>> getUnionTypeMap() {
            Map<Integer, Class<?>> result = new HashMap<>();
            result.put(5, SubclassA.class);
            return result;
        }
    }

    @Test
    public void testBadValueTypeInUnionTypeMap() throws B2JsonException {
        thrown.expectMessage("returned a map containing a class java.lang.Integer as a value");
        B2Json.get().fromJson("{}", UnionWithBadValueInMap.class);
    }

    @B2Json.union(typeField = "type")
    private static class UnionWithBadValueInMap {
        public static Map<String, Integer> getUnionTypeMap() {
            Map<String, Integer> result = new HashMap<>();
            result.put("a", 5);
            return result;
        }
    }

    @Test
    public void testUnionInheritsFromUnion() throws B2JsonException {
        thrown.expectMessage("inherits from another class with a B2Json annotation");
        B2Json.get().fromJson("{}", UnionThatInheritsFromUnion.class);
    }

    @B2Json.union(typeField = "type")
    private static class UnionThatInheritsFromUnion extends UnionAZ {}

    @Test
    public void testUnionMemberIsNotSubclass() throws B2JsonException {
        thrown.expectMessage("is not a subclass of");
        B2Json.get().fromJson("{}", UnionWithMemberThatIsNotSubclass.class);
    }

    @B2Json.union(typeField = "type")
    private static class UnionWithMemberThatIsNotSubclass {
        public static Map<String, Class<?>> getUnionTypeMap() {
            Map<String, Class<?>> result = new HashMap<>();
            result.put("doesNotInherit", SubclassDoesNotInherit.class);
            return result;
        }
    }

    private static class SubclassDoesNotInherit {
        @B2Json.constructor(params = "")
        private SubclassDoesNotInherit(int a) { }
    }

    @Test
    public void testUnionFieldHasDifferentTypes() throws B2JsonException {
        thrown.expectMessage("field sameName has two different types");
        B2Json.get().fromJson("{}", UnionXY.class);
    }

    @B2Json.union(typeField = "type")
    private static class UnionXY {
        public static Map<String, Class<?>> getUnionTypeMap() {
            Map<String, Class<?>> result = new HashMap<>();
            result.put("x", SubclassX.class);
            result.put("y", SubclassY.class);
            return result;
        }
    }

    private static class SubclassX extends UnionXY {
        @B2Json.required
        public final int sameName;

        @B2Json.constructor(params = "sameName")
        private SubclassX(int sameName) {
            this.sameName = sameName;
        }
    }

    private static class SubclassY extends UnionXY {
        @B2Json.required
        public final String sameName;

        @B2Json.constructor(params = "sameName")
        private SubclassY(String sameName) {
            this.sameName = sameName;
        }
    }

    @Test
    public void testUnionSubclassNotInTypeMap() throws B2JsonException {
        thrown.expectMessage("is not in the type map");
        B2Json.get().toJson(new SubclassM());
    }

    @B2Json.union(typeField = "type")
    private static class UnionM {
        public static Map<String, Class<?>> getUnionTypeMap() {
            Map<String, Class<?>> result = new HashMap<>();
            return result;
        }
    }

    private static class SubclassM extends UnionM {
    }
}
