/**
 * JSON (de)serialization of Java objects.
 *
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2StringUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

/**
 * <p>JSON (de)serialization of Java objects.</p>
 * <p>This class knows how to take a Java object and turn it
 * into JSON, and then reverse the process and take JSON and create
 * a Java object.</p>
 * <p>A number of classes are built in to B2Json.  In addition to all
 * of the primitive types (byte, char, int, etc.) these classes are
 * all handled: Byte, Character, Integer, Long, Float, Double, Boolean,
 * String, LocalDate, LocalDateTime, and BigDecimal.</p>
 * <p>For other classes to be used, you can either add a static getJsonTypeHandler()
 * method to it or add annotations to the class to say how it should go to JSON.
 * For classes without a getJsonTypeHandler() method, there must be a "required",
 * "optional", or "ignored" annotation on every field.  And there must be exactly
 * one constructor with the "constructor" annotation.</p>
 * <p>The selected constructor must take as arguments all of the non-ignored
 * fields in the object.  If any validation of values needs to happen
 * during deserialization, it should happen in the constructor.</p>
 * <p>During deserialization, an exception will be thrown if any required
 * fields are missing or null.  Optional fields are set to 0/false/null
 * if they are not present in the JSON.  If unexpected fields are present
 * in the JSON, they will cause an exception unless ALLOW_EXTRA_FIELDS
 * is selected.</p>
 * <p>java.util.Map objects turn into JSON objects when serialized, and
 * java.util.List and java.util.Set objects turn into JSON arrays.  On
 * deserialization, the values for Map fields are created as TreeMaps,
 * the values for List fields are created as ArrayLists, and the values for
 * Set fields are created as HashSets.</p>
 * <p>The JSON produced is always "pretty", with newlines and indentation.
 * Field names are always sorted alphabetically.</p>
 * <p>B2Json objects are THREAD SAFE.</p>
 */
public class B2Json {
    /* Design note.  If/when we need to handle types that we can't
     * annotate or add a getJsonTypeHandler() method to, we can
     * have some kind of plugin mechanism to register handlers with
     * this class.  And, before that, i might ask what you're doing
     * relying on the shape of a class for which you don't own the
     * source code?
     */
    private static String UTF8 = "UTF-8";

    /**
     * A simple instance that can be shared.
     */
    private static final B2Json instance = new B2Json();

    /**
     * Bit map values for the options parameter to the constructor.
     */
    public static final int ALLOW_EXTRA_FIELDS = 1;

    /**
     * The holder for all of the different handlers.
     */
    private final B2JsonHandlerMap handlerMap;

    /**
     * @return A shared instance.
     */
    public static B2Json get() {
        return instance;
    }

    /**
     * Initializes a new B2Json object with handlers for all of the
     * classes that are handled specially.
     */
    private B2Json() {
        this.handlerMap = new B2JsonHandlerMap();
    }

    /**
     * Turn an object into JSON, returning the result as an array of UTF-8
     * bytes.
     */
    public byte[] toJsonUtf8Bytes(Object obj) throws B2JsonException {
        try {
            return toJson(obj).getBytes(UTF8);
        } catch (IOException e) {
            throw new RuntimeException("error writing to byte array: " + e.getMessage());
        }
    }

    /**
     * Turn an object into JSON, returning the result as an array of UTF-8
     * bytes.
     */
    public byte[] toJsonUtf8BytesWithNewline(Object obj) throws B2JsonException {
        try {
            final String jsonWithNewline = toJson(obj) + "\n";
            return jsonWithNewline.getBytes(UTF8);
        } catch (IOException e) {
            throw new RuntimeException("error writing to byte array: " + e.getMessage());
        }
    }

    /**
     * Turn an object into JSON, writing the results to given
     * output stream.
     */
    public void toJson(Object obj, OutputStream out) throws IOException, B2JsonException {
        if (obj == null) {
            throw new B2JsonException("top level object must not be null");
        }
        final Class<?> clazz = obj.getClass();
        final B2JsonTypeHandler handler = handlerMap.getHandler(clazz);
        B2JsonWriter jsonWriter = new B2JsonWriter(out);
        //noinspection unchecked
        handler.serialize(obj, jsonWriter);
        jsonWriter.close();
    }

    /**
     * Turn an object into JSON, returning the result as a string.
     */
    public String toJson(Object obj) throws B2JsonException {
        if (obj == null) {
            throw new B2JsonException("top level object must not be null");
        }
        Class<?> clazz = obj.getClass();
        try {
            final B2JsonTypeHandler handler = handlerMap.getHandler(clazz);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            B2JsonWriter jsonWriter = new B2JsonWriter(out);
            //noinspection unchecked
            handler.serialize(obj, jsonWriter);
            jsonWriter.close();
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException("IO exception writing to string");
        }
    }

    /**
     * Turn an object into JSON, returning the result as a string.
     * This throws a RuntimeException instead of a B2JsonException,
     * so use it carefully.
     */
    public static String toJsonOrThrowRuntime(Object obj) {
        try {
            return get().toJson(obj);
        } catch (B2JsonException e) {
            throw new IllegalArgumentException("failed to convert to json: " + e.getMessage(), e);
        }
    }


    /**
     * Turn a map into JSON, returning the result as a string.
     */
    public String mapToJson(Map<?, ?> map, Class<?> keyClass, Class<?> valueClass) throws B2JsonException {
        if (map == null) {
            throw new B2JsonException("map must not be null");
        }
        try {
            final B2JsonTypeHandler keyHandler = handlerMap.getHandler(keyClass);
            final B2JsonTypeHandler valueHandler = handlerMap.getHandler(valueClass);
            final B2JsonTypeHandler handler = new B2JsonMapHandler(keyHandler, valueHandler);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            B2JsonWriter jsonWriter = new B2JsonWriter(out);
            //noinspection unchecked
            handler.serialize(map, jsonWriter);
            jsonWriter.close();
            return out.toString(B2StringUtil.UTF8);
        } catch (IOException e) {
            throw new RuntimeException("IO exception writing to string");
        }
    }

    /**
     * Parses a JSON object into a map.
     */
    public <K, V> Map<K, V> mapFromJson(String json, Class<K> keyClass, Class<V> valueClass) throws B2JsonException {
        final B2JsonTypeHandler keyHandler = handlerMap.getHandler(keyClass);
        final B2JsonTypeHandler valueHandler = handlerMap.getHandler(valueClass);
        final B2JsonTypeHandler handler = new B2JsonMapHandler(keyHandler, valueHandler);
        return fromJsonWithHandler(json, handler, 0);
    }

    /**
     * Turn a map into JSON, returning the result as a string.
     */
    public String listToJson(List<?> list, Class<?> valueClass) throws B2JsonException {
        if (list == null) {
            throw new B2JsonException("list must not be null");
        }
        try {
            final B2JsonTypeHandler valueHandler = handlerMap.getHandler(valueClass);
            final B2JsonTypeHandler handler = new B2JsonListHandler(valueHandler);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            B2JsonWriter jsonWriter = new B2JsonWriter(out);
            //noinspection unchecked
            handler.serialize(list, jsonWriter);
            jsonWriter.close();
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException("IO exception writing to string");
        }
    }

    /**
     * Parses a JSON object into a map.
     */
    public <V> List<V> listFromJson(String json, Class<V> valueClass) throws B2JsonException {
        final B2JsonTypeHandler valueHandler = handlerMap.getHandler(valueClass);
        final B2JsonTypeHandler handler = new B2JsonListHandler(valueHandler);
        return fromJsonWithHandler(json, handler, 0);
    }

    /**
     * Parse JSON as an object of the given class with the given options,
     * reading from the input stream until reaching EOF.  Throws an error
     * if there is anything but whitespace after the JSON value.
     */
    public <T> T fromJsonUntilEof(InputStream in, Class<T> clazz) throws IOException, B2JsonException {
        return fromJsonUntilEof(in, clazz, 0);
    }

    public <T> T fromJsonUntilEof(InputStream in, Class<T> clazz, int options) throws IOException, B2JsonException {
        B2JsonReader reader = new B2JsonReader(new InputStreamReader(in, "UTF-8"));
        final B2JsonTypeHandler handler = handlerMap.getHandler(clazz);
        //noinspection unchecked
        T result = (T) handler.deserialize(reader, options);
        if (!reader.nextNonWhitespaceIsEof()) {
            throw new B2JsonException("non-whitespace characters after JSON value");
        }
        return result;
    }

    /**
     * Parse JSON as an object of the given class with the given options.
     */
    public <T> T fromJson(InputStream in, Class<T> clazz) throws IOException, B2JsonException {
        return fromJson(in, clazz, 0);
    }

    public <T> T fromJson(InputStream in, Class<T> clazz, int options) throws IOException, B2JsonException {
        B2JsonReader reader = new B2JsonReader(new InputStreamReader(in, "UTF-8"));
        final B2JsonTypeHandler handler = handlerMap.getHandler(clazz);

        if (handler == null) {
            throw new B2JsonException("B2Json.fromJson called with handler not in handlerMap");

        }
        //noinspection unchecked
        return (T) handler.deserialize(reader, options);
    }

    /**
     * Parse JSON as an object of the given class.
     */
    public <T> T fromJson(String json, Class<T> clazz) throws B2JsonException {
        return fromJson(json, clazz, 0);
    }

    public <T> T fromJson(String json, Class<T> clazz, int options) throws B2JsonException {
        final B2JsonTypeHandler handler = handlerMap.getHandler(clazz);
        return fromJsonWithHandler(json, handler, options);
    }

    private <T> T fromJsonWithHandler(String json, B2JsonTypeHandler handler, int options) throws B2JsonException {
        try {
            B2JsonReader reader = new B2JsonReader(new StringReader(json));
            //noinspection unchecked
            return (T) handler.deserialize(reader, options);
        } catch (IOException e) {
            throw new RuntimeException("error reading string", e);
        }
    }

    /**
     * Parse JSON as an object of the given class.
     */
    public <T> T fromJson(byte[] jsonUtf8Bytes, Class<T> clazz) throws IOException, B2JsonException {
        return fromJson(jsonUtf8Bytes, clazz, 0);
    }

    public <T> T fromJson(byte[] jsonUtf8Bytes, Class<T> clazz, int options) throws IOException, B2JsonException {
        B2JsonReader reader = new B2JsonReader(new InputStreamReader(new ByteArrayInputStream(jsonUtf8Bytes), "UTF-8"));
        final B2JsonTypeHandler handler = handlerMap.getHandler(clazz);
        //noinspection unchecked
        return (T) handler.deserialize(reader, options);
    }

    /**
     * Parse a URL parameter map as an object of the given class.
     *
     * The values in the map are the values that will be used in the
     * object.  The caller is responsible for URL-decoding them
     * before passing them to this method.
     */
    public <T> T fromUrlParameterMap(Map<String, String> parameterMap, Class<T> clazz) throws IOException, B2JsonException {
        return fromUrlParameterMap(parameterMap, clazz, 0);
    }
    public <T> T fromUrlParameterMap(Map<String, String> parameterMap, Class<T> clazz, int options) throws IOException, B2JsonException {
        final B2JsonTypeHandler handler = handlerMap.getHandler(clazz);

        if (!(handler instanceof B2JsonObjectHandler)) {
            throw new B2JsonException("only objects can be deserialized from parameter maps");
        }
        @SuppressWarnings("unchecked")
        final B2JsonObjectHandler<T> objectHandler = (B2JsonObjectHandler<T>) handler;

        //noinspection unchecked
        return objectHandler.deserializeFromUrlParameterMap(parameterMap, options);
    }

    /**
     * Field annotation that says a field is required to be present.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface required {}

    /**
     * Field annotation that says a field is optional.  The value will
     * always be included, even if it is null.  When deserializing,
     * null/false/0 will be passed to the constructor if the value is
     * not present in the JSON.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface optional {}

    /**
     * Field annotation that says a field is optional.  The value will
     * always be included when serializing, even if it is null.  When
     * deserializing, the provided default value will be used.  The default
     * provided should be the JSON form of the value, as a string.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface optionalWithDefault {
        String defaultValue();
    }

    /**
     * Field annotation that says the field is not included in JSON.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ignored {}

    /**
     * Constructor annotation saying that this is the constructor B2Json
     * should use.  This constructor must take ALL of the serializable
     * fields as parameters.
     *
     * You must provide an "params" parameter that lists the order of
     * the parameters to the constructor.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.CONSTRUCTOR)
    public @interface constructor {
        String params();
        String discards() default "";
    }


    /**
     * Field annotation that designates the enum value to use when the
     * value in a field isn't one of the known values.  Use this at most
     * once in an enum.  If no values have this annotation, we will throw
     * a B2JsonException at runtime when we hit an invalid value.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface defaultForInvalidEnumValue {}
}
