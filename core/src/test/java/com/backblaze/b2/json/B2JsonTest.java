/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2Preconditions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
public class B2JsonTest extends B2BaseTest {

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

    private static final B2Json b2Json = B2Json.get();

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
    public void testBigInteger() throws IOException, B2JsonException {
        checkDeserializeSerialize("13", BigInteger.class);
        checkDeserializeSerialize("31415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679821480865132823066470", BigDecimal.class);
        checkDeserializeSerialize("-271828182845904523536028747135266249775724709369995957496696762772407663035354759457138217852516642742746", BigDecimal.class);
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
        assertEquals(json, b2Json.toJson(obj));
        assertEquals(obj, b2Json.fromJson(json, Container.class));
    }

    @Test
    public void testUnionWithTypeFieldLast() throws IOException, B2JsonException {
        final String json =
                "{\n" +
                        "  \"a\": 5,\n" +
                        "  \"b\": null,\n" +
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

    /**
     * Regression test to make sure that when handlers are created from
     * B2JsonUnionHandler they work properly.
     *
     * This test makes a new B2Json, and de-serializes the union type, so
     * it's sure that the B2JsonUnionBaseHandler gets initialized first,
     * which is what triggered the bug.
     */
    @Test
    public void testUnionCreatesHandlers() throws B2JsonException {
        (new B2Json()).fromJson("{\n" +
                "  \"type\": \"a\",\n" +
                "  \"a\": 5,\n" +
                "  \"b\": [10]" +
                "}",
                UnionAZ.class
        );
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
        assertEquals(jsonWithoutComment, b2Json.toJson(obj));
        assertEquals(obj, b2Json.fromJson(json, Container.class));
    }

    @Test
    public void testNoCommentInString() throws B2JsonException {
        String json =
                "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"he//o\"\n" +
                "}";
        Container obj = new Container(41, "he//o");
        assertEquals(json, b2Json.toJson(obj));
        assertEquals(obj, b2Json.fromJson(json, Container.class));
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
        b2Json.fromJson(json, Container.class);
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
        b2Json.fromJson(json, Container.class);
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
        b2Json.fromJson(json, Container.class);
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
        b2Json.fromJson(json, Container.class);
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
        b2Json.fromJson(json, Container.class);
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
        b2Json.fromJson(json, Container.class);
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

        Container c = b2Json.fromJson(json, Container.class, B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS);

        String expectedJson =
                "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\"\n" +
                "}";
        assertEquals(expectedJson, b2Json.toJson(c));
    }

    @Test
    public void testAllowButSkipDiscarded() throws B2JsonException {
        final String jsonWithExtra = "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\",\n" +
                "  \"c\": 7" +
                "}";

        final Discarder discarder = b2Json.fromJson(jsonWithExtra, Discarder.class);
        assertEquals(41, discarder.a);
        assertEquals(7, discarder.c);

        final String expectedJson = "{\n" +
                "  \"a\": 41,\n" +
                "  \"c\": 7\n" +
                "}";
        assertEquals(expectedJson, b2Json.toJson(discarder));
    }

    @Test
    public void testDiscardingIgnoredFieldIsOk() throws B2JsonException {
        final String jsonWithExtra = "{\n" +
                "  \"a\": 41,\n" +
                "  \"b\": \"hello\",\n" +
                "  \"c\": 7" +
                "}";

        final DiscardingIgnoredFieldIsOk discarder = b2Json.fromJson(jsonWithExtra, DiscardingIgnoredFieldIsOk.class);
        assertEquals(41, discarder.a);
        assertEquals(42, discarder.c); // 'cuz ignored from json and set by constructor.

        final String expectedJson = "{\n" +
                "  \"a\": 41\n" +
                "}";
        assertEquals(expectedJson, b2Json.toJson(discarder));
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

        b2Json.fromJson(jsonWithExtra, DiscardingNonIgnoredFieldIsIllegal.class);
    }


    @Test
    public void testMissingRequired() throws B2JsonException {
        String json = "{ \"b\" : \"hello\" }";

        thrown.expect(B2JsonException.class);
        thrown.expectMessage("required field a is missing");
        b2Json.fromJson(json, Container.class);
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
        AllOptionalTypes obj = b2Json.fromJson(json, AllOptionalTypes.class);
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
        assertEquals(expectedJson, b2Json.toJson(obj));

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
        AllOptionalTypes obj = b2Json.fromUrlParameterMap(parameterMap, AllOptionalTypes.class);
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
        AllOptionalTypes obj = b2Json.fromJson(json, AllOptionalTypes.class);
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
        b2Json.toJson(null);
    }

    @Test
    public void testSerializeNullRequired() throws B2JsonException {
        RequiredObject obj = new RequiredObject(null);

        thrown.expect(B2JsonException.class);
        thrown.expectMessage("required field a cannot be null");
        b2Json.toJson(obj);
    }

    @Test
    public void testDeserializeNullRequired() throws B2JsonException {
        String json = "{ \"a\" : null }";

        thrown.expect(B2JsonException.class);
        thrown.expectMessage("required field a cannot be null");
        b2Json.fromJson(json, RequiredObject.class);
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
        T obj = b2Json.fromJson(json, clazz);
        assertEquals(json, b2Json.toJson(obj));

        byte [] bytes = getUtf8Bytes(json);
        T obj2 = b2Json.fromJson(bytes, clazz);
        assertArrayEquals(bytes, b2Json.toJsonUtf8Bytes(obj2));

        T obj3 = b2Json.fromJson(bytes, clazz);
        byte [] bytesWithNewline = getUtf8Bytes(json + "\n");
        assertArrayEquals(bytesWithNewline, b2Json.toJsonUtf8BytesWithNewline(obj3));
    }

    private <T> void checkDeserializeSerialize(String json, Class<T> clazz, String expectedJson) throws IOException, B2JsonException {
        T obj = b2Json.fromJson(json, clazz);
        assertEquals(expectedJson, b2Json.toJson(obj));

        byte [] bytes = getUtf8Bytes(json);
        T obj2 = b2Json.fromJson(bytes, clazz);
        assertArrayEquals(getUtf8Bytes(expectedJson), b2Json.toJsonUtf8Bytes(obj2));
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
            b2Json.toJson(mapWithNullKeyHolder);
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
        assertEquals(json, b2Json.toJson(holder));
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
        assertEquals(json, b2Json.mapToJson(map, String.class, Integer.class));
        assertEquals(map, b2Json.mapFromJson(json, String.class, Integer.class));
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
        assertEquals(json, b2Json.listToJson(list, String.class));
        assertEquals(list, b2Json.listFromJson(json, String.class));
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
        assertEquals(str, b2Json.fromJson(json, String.class));

        // Check de-serializing from bytes
        assertEquals(str, b2Json.fromJson(new ByteArrayInputStream(utf8Json), String.class));

        // Check serializing to string
        assertEquals(json, b2Json.toJson(str));

        // Check serializing to bytes
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            b2Json.toJson(str, out);
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
        final String json = b2Json.toJson(orig);
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
        b2Json.fromJson(json, EvenNumber.class);
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

            public void serialize(GoodCustomHandler obj, B2JsonOptions options, B2JsonWriter out) throws IOException {
                out.writeString("GoodCustomHandler");
            }

            public GoodCustomHandler deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
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
            OptionalWithDefaultHolder actual = b2Json.fromJson("{}", OptionalWithDefaultHolder.class);
            assertEquals(expected, actual);
        }
        {
            OptionalWithDefaultHolder expected = new OptionalWithDefaultHolder(2, 3, "4");
            OptionalWithDefaultHolder actual = b2Json.fromJson("{\"a\": 2, \"b\": 3, \"c\": \"4\"}", OptionalWithDefaultHolder.class);
            assertEquals(expected, actual);
        }
    }

    private static class OptionalWithDefaultInvalidValue {
        @B2Json.optionalWithDefault(defaultValue = "xxx")
        private final int count;

        @B2Json.constructor(params = "count")
        private OptionalWithDefaultInvalidValue(int count) {
            this.count = count;
        }
    }

    @Test
    public void testInvalidValueInOptionalWithDefault() throws B2JsonException {
        // Any use of the class with B2Json should trigger the exception.  Even
        // serializing will need to initialize the handler, which should trigger
        // an error.
        thrown.expectMessage("error in default value for OptionalWithDefaultInvalidValue.count: Bad number");
        B2Json.get().toJson(new OptionalWithDefaultInvalidValue(0));
    }

    @Test
    public void testVersionRangeBackwards() throws B2JsonException {
        thrown.expectMessage("last version 1 is before first version 2 in class com.backblaze.b2.json.B2JsonTest$VersionRangeBackwardsClass");
        b2Json.toJson(new VersionRangeBackwardsClass(5));
    }

    private static class VersionRangeBackwardsClass {
        @B2Json.required
        @B2Json.versionRange(firstVersion = 2, lastVersion = 1)
        private final int n;

        @B2Json.constructor(params =  "n")
        private VersionRangeBackwardsClass(int n) {
            this.n = n;
        }
    }

    @Test
    public void testConflictingVersions() throws B2JsonException {
        thrown.expectMessage("must not specify both 'firstVersion' and 'versionRange' in class com.backblaze.b2.json.B2JsonTest$VersionConflictClass");
        b2Json.toJson(new VersionConflictClass(5));
    }

    private static class VersionConflictClass {
        @B2Json.required
        @B2Json.firstVersion(firstVersion = 5)
        @B2Json.versionRange(firstVersion = 2, lastVersion = 1)
        private final int n;

        @B2Json.constructor(params =  "n")
        private VersionConflictClass(int n) {
            this.n = n;
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
            public void serialize(Letter obj, B2JsonOptions options, B2JsonWriter out) throws IOException {
                out.writeString("b");
            }

            @Override
            public Letter deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
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
        final String json = "{ \"badField\" : 5, \"type\": \"a\" }";
        thrown.expectMessage("unknown field 'badField' in union type UnionAZ");
        B2Json.get().fromJson(json, UnionAZ.class);
    }

    @Test
    public void testSerializeUnionSubType() throws B2JsonException, IOException {
        final String origJson = "{\n" +
                "  \"contained\": {\n" +
                "    \"a\": 1,\n" +
                "    \"b\": null,\n" +
                "    \"type\": \"a\"\n" +
                "  }\n" +
                "}";
        checkDeserializeSerialize(origJson, ContainsUnion.class);
    }

    @Test
    public void testSerializeOptionalAndMissingUnion() throws B2JsonException, IOException {
        final String origJson = "{\n" +
                "  \"contained\": null\n" +
                "}";
        checkDeserializeSerialize(origJson, ContainsOptionalUnion.class);
    }

    @Test
    public void testSerializeUnregisteredUnionSubType() throws B2JsonException {
        final SubclassUnregistered unregistered = new SubclassUnregistered("zzz");
        final ContainsUnion container = new ContainsUnion(unregistered);
        thrown.expectMessage("class com.backblaze.b2.json.B2JsonTest$SubclassUnregistered isn't a registered part of union class com.backblaze.b2.json.B2JsonTest$UnionAZ");
        B2Json.get().toJson(container);
    }

    @B2Json.union(typeField = "type")
    @B2Json.defaultForUnknownType(value = "{\"type\": \"a\", \"n\": 5}")
    private static class UnionWithDefault {
        public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
            return B2JsonUnionTypeMap
                    .builder()
                    .put("a", UnionWithDefaultClassA.class)
                    .build();
        }
    }

    private static class UnionWithDefaultClassA extends UnionWithDefault {
        @B2Json.required
        private final int n;

        @B2Json.constructor(params = "n")
        private UnionWithDefaultClassA(int n) {
            this.n = n;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UnionWithDefaultClassA that = (UnionWithDefaultClassA) o;
            return n == that.n;
        }

        @Override
        public int hashCode() {
            return Objects.hash(n);
        }
    }

    @Test
    public void testUnionWithDefault() throws B2JsonException {
        assertEquals(
                new UnionWithDefaultClassA(5), // the default value
                B2Json.get().fromJson("{\"type\": \"unknown\"}", UnionWithDefault.class)
        );
        assertEquals(
                new UnionWithDefaultClassA(5), // the default value
                B2Json.get().fromJson("{\"type\": \"unknown\", \"unknownField\": 5}", UnionWithDefault.class)
        );
        assertEquals(
                new UnionWithDefaultClassA(99), // NOT the default value; the value provided
                B2Json.get().fromJson("{\"type\": \"a\", \"n\": 99}", UnionWithDefault.class)
        );
    }

    @B2Json.union(typeField = "type")
    @B2Json.defaultForUnknownType(value = "{\"type\": \"a\", \"n\": 5}")
    private static class UnionWithInvalidDefault {
        public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
            return B2JsonUnionTypeMap
                    .builder()
                    .put("a", UnionWithInvalidDefaultClassA.class)
                    .build();
        }
    }

    private static class UnionWithInvalidDefaultClassA extends UnionWithInvalidDefault {
        @B2Json.constructor(params = "")
        UnionWithInvalidDefaultClassA() {}
    }

    @Test
    public void testUnionWithInvalidDefault() throws B2JsonException {
        // The error should be caught when the class is first used, even if the default
        // isn't used.
        thrown.expectMessage("error in default value for union UnionWithInvalidDefault: unknown field 'n' in union type UnionWithInvalidDefault");
        B2Json.get().toJson(new UnionWithInvalidDefaultClassA());
    }

    @B2Json.union(typeField = "type")
    private static class UnionAZ {
        public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
            return B2JsonUnionTypeMap
                    .builder()
                    .put("a", SubclassA.class)
                    .put("z", SubclassZ.class)
                    .build();
        }
    }

    private static class SubclassA extends UnionAZ {
        @B2Json.required
        public final int a;

        @B2Json.optional
        public final Set<Integer> b;

        @B2Json.constructor(params = "a, b")
        private SubclassA(int a, Set<Integer> b) {
            this.a = a;
            this.b = b;
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

    private static class SubclassUnregistered extends UnionAZ {
        @B2Json.required
        public final String u;

        @B2Json.constructor(params = "u")
        private SubclassUnregistered(String u) {
            this.u = u;
        }
    }

    // i *soooo* wanted to call this class "North",
    // but the more boring name "ContainsUnion" is clearer.
    private static class ContainsUnion {
        @B2Json.required
        private final UnionAZ contained;

        @B2Json.constructor(params = "contained")
        private ContainsUnion(UnionAZ contained) {
            this.contained = contained;
        }
    }

    private static class ContainsOptionalUnion {
        @B2Json.optional
        private final UnionAZ contained;

        @B2Json.constructor(params = "contained")
        private ContainsOptionalUnion(UnionAZ contained) {
            this.contained = contained;
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
        thrown.expectMessage("UnionWithNonMap.getUnionTypeMap() did not return a B2JsonUnionTypeMap");
        B2Json.get().fromJson("{}", UnionWithNonMap.class);
    }

    @B2Json.union(typeField = "type")
    private static class UnionWithNonMap {
        public static String getUnionTypeMap() {
            return "foo";
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
        public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
            return B2JsonUnionTypeMap
                    .builder()
                    .put("doesNotInherit", SubclassDoesNotInherit.class)
                    .build();
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
        public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
            return B2JsonUnionTypeMap
                    .builder()
                    .put("x", SubclassX.class)
                    .put("y", SubclassY.class)
                    .build();
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
        public static B2JsonUnionTypeMap getUnionTypeMap() {
            return B2JsonUnionTypeMap.builder().build();
        }
    }

    private static class SubclassM extends UnionM {
    }

    @Test
    public void testUnionMapHasDuplicateName() throws B2JsonException {
        thrown.expectMessage("duplicate type name in union type map: 'd'");
        B2Json.get().toJson(new SubclassD1());
    }

    @B2Json.union(typeField = "type")
    private static class UnionD {
        public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
            return B2JsonUnionTypeMap
                    .builder()
                    .put("d", SubclassD1.class)
                    .put("d", SubclassD2.class)
                    .build();
        }
    }

    private static class SubclassD1 extends UnionD {
    }

    private static class SubclassD2 extends UnionD {
    }

    @Test
    public void testUnionMapHasDuplicateClass() throws B2JsonException {
        thrown.expectMessage("duplicate class in union type map: class com.backblaze.b2.json.B2JsonTest$SubclassF");
        B2Json.get().toJson(new SubclassF());
    }

    @B2Json.union(typeField = "type")
    private static class UnionF {
        public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
            return B2JsonUnionTypeMap
                    .builder()
                    .put("f1", SubclassF.class)
                    .put("f2", SubclassF.class)
                    .build();
        }
    }

    private static class SubclassF extends UnionF {
    }


    @Test
    public void testUnionSubclassHasNullOptionalField() throws B2JsonException, IOException {
        final String json = "{\n" +
                "  \"name\": null,\n" +
                "  \"type\": \"g\"\n" +
                "}";
        checkDeserializeSerialize(json, UnionG.class);
    }

    @B2Json.union(typeField = "type")
    private static class UnionG {
        public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
            return B2JsonUnionTypeMap
                    .builder()
                    .put("g", SubclassG.class)
                    .build();
        }
    }

    private static class SubclassG extends UnionG {
        @B2Json.optional
        final String name;

        @B2Json.constructor(params = "name")
        private SubclassG(String name) {
            this.name = name;
        }
    }

    @Test
    public void testUnionSubclassHasNullRequiredField() throws B2JsonException, IOException {
        final String json = "{\n" +
                "  \"name\": null,\n" +
                "  \"type\": \"h\"\n" +
                "}";
        thrown.expectMessage("required field name cannot be null");
        checkDeserializeSerialize(json, UnionH.class);
    }

    @B2Json.union(typeField = "type")
    private static class UnionH {
        public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
            return B2JsonUnionTypeMap
                    .builder()
                    .put("h", SubclassH.class)
                    .build();
        }
    }

    private static class SubclassH extends UnionH {
        @B2Json.required
        final String name;

        @B2Json.constructor(params = "name")
        private SubclassH(String name) {
            this.name = name;
        }
    }

    @Test
    public void testRequiredFieldNotInVersion() throws B2JsonException {
        final String json = "{}";
        final B2JsonOptions options = B2JsonOptions.builder().setVersion(1).build();
        final VersionedContainer obj = b2Json.fromJson(json, VersionedContainer.class, options);
        assertEquals(0, obj.x);
        assertEquals(1, obj.version);
    }

    @Test
    public void testRequiredFieldMissingInVersion() throws B2JsonException {
        final String json = "{}";
        final B2JsonOptions options = B2JsonOptions.builder().setVersion(5).build();

        thrown.expectMessage("required field x is missing");
        b2Json.fromJson(json, VersionedContainer.class, options);
    }

    @Test
    public void testFieldPresentButNotInVersion() throws B2JsonException {
        final String json = "{ \"x\": 5 }";
        final B2JsonOptions options = B2JsonOptions.builder().setVersion(1).build();

        thrown.expectMessage("field x is not in version 1");
        b2Json.fromJson(json, VersionedContainer.class, options);
    }

    @Test
    public void testFieldPresentAndInVersion() throws B2JsonException {
        final String json = "{ \"x\": 7 }";
        final B2JsonOptions options = B2JsonOptions.builder().setVersion(5).build();
        final VersionedContainer obj = b2Json.fromJson(json, VersionedContainer.class, options);
        assertEquals(7, obj.x);
        assertEquals(5, obj.version);
    }

    @Test
    public void testSerializeSkipFieldNotInVersion() throws B2JsonException {
        // Version 3 is too soon
        {
            final B2JsonOptions options = B2JsonOptions.builder().setVersion(3).build();
            assertEquals(
                    "{}",
                    b2Json.toJson(new VersionedContainer(3, 5), options)
            );
        }

        // Version 4 is the first version where it's present
        {
            final B2JsonOptions options = B2JsonOptions.builder().setVersion(4).build();
            assertEquals(
                    "{\n  \"x\": 3\n}",
                    b2Json.toJson(new VersionedContainer(3, 5), options)
            );
        }

        // Version 6 is the last version where it's present
        {
            final B2JsonOptions options = B2JsonOptions.builder().setVersion(4).build();
            assertEquals(
                    "{\n  \"x\": 3\n}",
                    b2Json.toJson(new VersionedContainer(3, 5), options)
            );
        }

        // Version 7 is too late.
        {
            final B2JsonOptions options = B2JsonOptions.builder().setVersion(7).build();
            assertEquals(
                    "{}",
                    b2Json.toJson(new VersionedContainer(3, 5), options)
            );
        }
    }

    @Test
    public void testSerializeIncludeFieldInVersion() throws B2JsonException {
        final B2JsonOptions options = B2JsonOptions.builder().setVersion(5).build();
        assertEquals(
                "{\n" +
                "  \"x\": 3\n" +
                "}",
                b2Json.toJson(new VersionedContainer(3, 5), options)
        );
    }


    private static class VersionedContainer {
        @B2Json.versionRange(firstVersion = 4, lastVersion = 6)
        @B2Json.required
        public final int x;

        @B2Json.ignored
        public final int version;

        @B2Json.constructor(params = "x, v", versionParam = "v")
        public VersionedContainer(int x, int v) {
            this.x = x;
            this.version = v;
        }
    }

    @Test
    public void testParamListedTwice() throws B2JsonException {
        final B2JsonOptions options = B2JsonOptions.builder().build();
        thrown.expectMessage("com.backblaze.b2.json.B2JsonTest$ConstructorParamListedTwice constructor parameter 'a' listed twice");
        b2Json.fromJson("{}", ConstructorParamListedTwice.class, options);
    }

    private static class TestClassOne {
        @B2Json.versionRange(firstVersion = 1, lastVersion = 3)
        @B2Json.required
        final String name;

        @B2Json.required
        final int number;

        @B2Json.constructor(params = "name, number")
        private TestClassOne(String name, int number) {
            this.name = name;
            this.number = number;
        }
    }

    @Test
    public void testToJson() {
        final TestClassOne testClassOne = new TestClassOne("testABC", 1);
        final String testClassOneStr = B2Json.toJsonOrThrowRuntime(testClassOne);
        assertEquals("{\n  \"name\": \"testABC\",\n  \"number\": 1\n}", testClassOneStr);
    }

    @Test
    public void testToJsonWithDefaultOptions() {
        final B2JsonOptions options = B2JsonOptions.builder().build();
        final TestClassOne testClassOne = new TestClassOne("testABC", 1);
        final String testClassOneStr = B2Json.toJsonOrThrowRuntime(testClassOne, options);
        assertEquals("{\n  \"name\": \"testABC\",\n  \"number\": 1\n}", testClassOneStr);
    }

    @Test
    public void testToJsonWithOptions() {
        // name is only in first 3 versions
        final B2JsonOptions options = B2JsonOptions.builder().setVersion(4).build();
        final TestClassOne testClassOne = new TestClassOne("testABC", 1);
        final String testClassOneStr = B2Json.toJsonOrThrowRuntime(testClassOne, options);
        assertEquals("{\n  \"number\": 1\n}", testClassOneStr);
    }

    @Test
    public void testToJsonThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert to json: required field a cannot be null");
        RequiredObject obj = new RequiredObject(null);
        B2Json.toJsonOrThrowRuntime(obj);
    }

    private static class SecureContainer {
        @B2Json.required
        @B2Json.sensitive
        private final String sensitiveString;

        @B2Json.required
        private final String insensitiveString;

        @B2Json.constructor(params = "sensitiveString,insensitiveString")
        public SecureContainer(String secureString, String insecureString) {
            this.sensitiveString = secureString;
            this.insensitiveString = insecureString;
        }
    }

    @Test
    public void testSensitiveRedactedWhenOptionSet() {
        final B2JsonOptions options = B2JsonOptions.builder().setRedactSensitive(true).build();
        final SecureContainer secureContainer = new SecureContainer("foo", "bar");
        assertEquals("{\n  \"insensitiveString\": \"bar\",\n  \"sensitiveString\": \"***REDACTED***\"\n}",
                B2Json.toJsonOrThrowRuntime(secureContainer, options));
    }

    @Test
    public void testSensitiveWrittenWhenOptionNotSet() {
        final B2JsonOptions options = B2JsonOptions.builder().setRedactSensitive(false).build();
        final SecureContainer secureContainer = new SecureContainer("foo", "bar");
        assertEquals("{\n  \"insensitiveString\": \"bar\",\n  \"sensitiveString\": \"foo\"\n}",
                B2Json.toJsonOrThrowRuntime(secureContainer, options));
    }

    private static class OmitNullBadTestClass {
        @B2Json.optional(omitNull = true)
        private final int omitNullInt;

        @B2Json.constructor(params = "omitNullInt")
        public OmitNullBadTestClass(int omitNullInt) {
            this.omitNullInt = omitNullInt;
        }
    }

    private static class OmitNullTestClass {
        @B2Json.optional(omitNull = true)
        private final String omitNullString;

        @B2Json.optional
        private final String regularString;

        @B2Json.optional(omitNull = true)
        private final Integer omitNullInteger;

        @B2Json.optional
        private final Integer regularInteger;

        @B2Json.constructor(params = "omitNullString, regularString, omitNullInteger, regularInteger")
        public OmitNullTestClass(String omitNullString, String regularString, Integer omitNullInteger, Integer regularInteger) {
            this.omitNullString = omitNullString;
            this.regularString = regularString;
            this.omitNullInteger = omitNullInteger;
            this.regularInteger = regularInteger;
        }
    }

    @Test
    public void testOmitNullWithNullInputs() {
        final OmitNullTestClass object = new OmitNullTestClass(null, null, null, null);
        final String actual = B2Json.toJsonOrThrowRuntime(object);

        // The omitNullString and omitNullInteger fields should not be present in the output
        assertEquals("{\n" +
                "  \"regularInteger\": null,\n" +
                "  \"regularString\": null\n" +
                "}", actual);
    }

    @Test
    public void testOmitNullWithNonNullInputs() {
        final OmitNullTestClass object = new OmitNullTestClass("foo", "bar", 1, 1);
        final String actual = B2Json.toJsonOrThrowRuntime(object);

        // All the fields should be in the output
        assertEquals("{\n" +
                "  \"omitNullInteger\": 1,\n" +
                "  \"omitNullString\": \"foo\",\n" +
                "  \"regularInteger\": 1,\n" +
                "  \"regularString\": \"bar\"\n" +
                "}", actual);
    }

    @Test
    public void testOmitNullCreateFromEmpty() {
        final OmitNullTestClass actual = B2Json.fromJsonOrThrowRuntime("{}", OmitNullTestClass.class);

        assertNull(actual.omitNullString);
        assertNull(actual.regularString);
        assertNull(actual.omitNullInteger);
        assertNull(actual.regularInteger);
    }

    @Test
    public void testOmitNullOnPrimitive() throws B2JsonException {
        thrown.expectMessage("Field OmitNullBadTestClass.omitNullInt declared with 'omitNull = true' but is a primitive type");
        final OmitNullBadTestClass bad = new OmitNullBadTestClass(123);
        B2Json.toJsonOrThrowRuntime(bad);
    }

    /**
     * Because of serialization, the object returned from B2Json will never be the same object as an
     * instantiated one.
     *
     * So we just look at and test the members.
     */
    @Test
    public void testFromJson() {
        final String testClassOneStr = "{\n \"name\": \"testABC\",\n \"number\": 1\n}";
        final TestClassOne testClassOne = B2Json.fromJsonOrThrowRuntime(testClassOneStr, TestClassOne.class);

        assertEquals("testABC", testClassOne.name);
        assertEquals(1, testClassOne.number);
    }

    @Test
    public void testFromJsonWithDefaultOptions() {
        final B2JsonOptions options = B2JsonOptions.builder().build();
        final String testClassOneStr = "{\n \"name\": \"testABC\",\n \"number\": 1\n}";
        final TestClassOne testClassOne = B2Json.fromJsonOrThrowRuntime(testClassOneStr, TestClassOne.class, options);

        assertEquals("testABC", testClassOne.name);
        assertEquals(1, testClassOne.number);
    }

    @Test
    public void testFromJsonWithOptions() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert from json: field name is not in version 6");
        final B2JsonOptions options = B2JsonOptions.builder().setVersion(6).build();
        final String testClassOneStr = "{\n \"name\": \"testABC\",\n \"number\": 1\n}";
        final TestClassOne testClassOne = B2Json.fromJsonOrThrowRuntime(testClassOneStr, TestClassOne.class, options);
    }

    @Test
    public void testFromJsonThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("failed to convert from json: required field number is missing");
        final String testClassOneStr = "{\n \"name\": \"testABC\"\n}";
        B2Json.fromJsonOrThrowRuntime(testClassOneStr, TestClassOne.class);
    }

    private static class ConstructorParamListedTwice {
        @B2Json.required
        public final int a;

        @B2Json.optional
        public final int b;

        @B2Json.constructor(params = "a, a")
        public ConstructorParamListedTwice(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    @Test
    public void testRecursiveUnion() {
        final String json = B2Json.toJsonOrThrowRuntime(new RecursiveUnionNode(new RecursiveUnionNode(null)));
        B2Json.fromJsonOrThrowRuntime(json, RecursiveUnion.class);
    }

    @B2Json.union(typeField = "type")
    private static class RecursiveUnion {
        public static B2JsonUnionTypeMap getUnionTypeMap() throws B2JsonException {
            return B2JsonUnionTypeMap
                    .builder()
                    .put("node", RecursiveUnionNode.class)
                    .build();
        }
    }

    private static class RecursiveUnionNode extends RecursiveUnion {

        @B2Json.optional
        private final RecursiveUnion recursiveUnion;

        @B2Json.constructor(params = "recursiveUnion")
        private RecursiveUnionNode(RecursiveUnion recursiveUnion) {
            this.recursiveUnion = recursiveUnion;
        }
    }

    private static class CharSquenceTestClass {
        @B2Json.required
        CharSequence sequence;

        @B2Json.constructor(params = "sequence")
        public CharSquenceTestClass(CharSequence sequence) {
            this.sequence = sequence;
        }
    }

    @Test
    public void testCharSequenceSerialization() {
        final CharSequence sequence ="foobarbaz".subSequence(3, 6);

        final CharSquenceTestClass obj = new CharSquenceTestClass(sequence);

        final String actual = B2Json.toJsonOrThrowRuntime(obj);

        final String expected = "{\n" +
                "  \"sequence\": \"bar\"\n" +
                "}";

        assertEquals(expected, actual);
    }

    @Test
    public void testCharSequenceDeserialization() {
        final String input = "{\n" +
                "  \"sequence\": \"bar\"\n" +
                "}";

        final CharSquenceTestClass obj = B2Json.fromJsonOrThrowRuntime(input, CharSquenceTestClass.class);

        assertEquals("bar", obj.sequence);
        // the underlying implementation of CharSequence that we deserialize is String
        assertEquals(String.class, obj.sequence.getClass());
    }
}
