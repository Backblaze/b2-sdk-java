/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2StringUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.backblaze.b2.json.B2JsonBoundedByteArrayOutputStream.SYSTEM_MAX_CAPACITY;

/**
 * <p>JSON (de)serialization of Java objects.</p>
 *
 * <p>This class knows how to take a Java object and turn it
 * into JSON, and then reverse the process and take JSON and create
 * a Java object.</p>
 *
 * <p>A number of classes are built in to B2Json.  In addition to all
 * of the primitive types (byte, char, int, etc.) these classes are
 * all handled: Byte, Character, Integer, Long, Float, Double, Boolean,
 * String, LocalDate, LocalDateTime, and BigDecimal.</p>
 *
 * <p>For other classes to be used, you can either add a static getJsonTypeHandler()
 * method to it or add annotations to the class to say how it should go to JSON.
 * For classes without a getJsonTypeHandler() method, there must be a "required",
 * "optional", or "ignored" annotation on every field.  And there must be exactly
 * one constructor with the "constructor" annotation.</p>
 *
 * <p>The selected constructor must take as arguments all of the non-ignored
 * fields in the object.  If any validation of values needs to happen
 * during deserialization, it should happen in the constructor.</p>
 *
 * <p>During deserialization, an exception will be thrown if any required
 * fields are missing or null.  Optional fields are set to 0/false/null
 * if they are not present in the JSON.  If unexpected fields are present
 * in the JSON, they will cause an exception unless ALLOW_EXTRA_FIELDS
 * is selected.</p>
 *
 * <p>java.util.Map objects turn into JSON objects when serialized, and
 * java.util.List and java.util.Set objects turn into JSON arrays.  On
 * deserialization, the values for Map fields are created as TreeMaps,
 * the values for List fields are created as ArrayLists, and the values for
 * Set fields are created as HashSets.</p>
 *
 * <p>The JSON produced is always "pretty", with newlines and indentation.
 * Field names are always sorted alphabetically.</p>
 *
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
     *
     * Deprecated in favor of using B2JsonOptions.
     */
    @Deprecated
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
    /*testing*/ B2Json() {
        this.handlerMap = new B2JsonHandlerMap();
    }

    /**
     * Turn an object into JSON, returning the result as an array of UTF-8
     * bytes.
     */
    public byte[] toJsonUtf8Bytes(Object obj) throws B2JsonException {
        return toJsonUtf8Bytes(obj, B2JsonOptions.DEFAULT);
    }

    public byte[] toJsonUtf8Bytes(Object obj, B2JsonOptions options) throws B2JsonException {
        try {
            return toJson(obj, options).getBytes(UTF8);
        } catch (IOException e) {
            throw new RuntimeException("error writing to byte array: " + e.getMessage());
        }
    }

    /**
     * Turn an object into JSON, returning the result as an array of UTF-8
     * bytes.
     */
    public byte[] toJsonUtf8BytesWithNewline(Object obj) throws B2JsonException {
        return toJsonUtf8BytesWithNewline(obj, B2JsonOptions.DEFAULT);
    }

    public byte[] toJsonUtf8BytesWithNewline(Object obj, B2JsonOptions options) throws B2JsonException {
        try {
            final B2JsonBoundedByteArrayOutputStream out = new B2JsonBoundedByteArrayOutputStream(SYSTEM_MAX_CAPACITY);
            toJson(obj, options, out);
            out.write('\n');
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("error writing to byte array: " + e.getMessage());
        }
    }

    /**
     * Turn an object into JSON, writing the results to given output stream.
     */
    public void toJson(Object obj, OutputStream out) throws IOException, B2JsonException {
        toJson(obj, B2JsonOptions.DEFAULT, out);
    }

    /**
     * Turn an object into JSON, writing the results to given output stream and
     * using the supplied options.
     */
    public void toJson(Object obj, B2JsonOptions options, OutputStream out) throws IOException, B2JsonException {
        toJson(obj, options, out, null);
    }

    /**
     * Turn an object into JSON, writing the results to given
     * output stream.
     *
     * objTypeOrNull can be set to null if obj is not a parameterized class. However,
     * if obj contains type parameters (like if obj is a {@literal List<String>}, then
     * you will need to pass in its type information via objTypeOrNull. This will instruct
     * B2Json to derive the B2JsonTypeHandler from the type information instead of the
     * object's class.
     *
     * Getting the Type of obj can be done in at least two ways:
     * 1. If it is a member of an enclosing class, EnclosingClass.getDeclaredField(...).getGenericType()
     * 2. By constructing a class that implements Type.
     *
     * Note that the output stream is NOT closed as a side-effect of calling this.
     * It was a bug that it was being closed in version 1.1.1 and earlier.
     */
    public void toJson(Object obj, B2JsonOptions options, OutputStream out, Type objTypeOrNull)
            throws IOException, B2JsonException {

        if (obj == null) {
            throw new B2JsonException("top level object must not be null");
        }
        final B2JsonTypeHandler handler = objTypeOrNull == null ?
                handlerMap.getHandler(obj.getClass()) : handlerMap.getHandler(objTypeOrNull);
        B2JsonWriter jsonWriter = new B2JsonWriter(out, options);
        //noinspection unchecked
        handler.serialize(obj, options, jsonWriter);
    }

    /**
     * Turn an object into JSON, returning the result as a string.
     */
    public String toJson(Object obj) throws B2JsonException {
        return toJson(obj, B2JsonOptions.DEFAULT);
    }

    public String toJson(Object obj, B2JsonOptions options) throws B2JsonException {
        try (final B2JsonBoundedByteArrayOutputStream out = new B2JsonBoundedByteArrayOutputStream(SYSTEM_MAX_CAPACITY)) {
            toJson(obj, options, out);
            return out.toString(B2StringUtil.UTF8);
        } catch (IOException e) {
            throw new RuntimeException("IO exception writing to string: " + e.getMessage());
        }
    }

    /**
     * Turn an object into JSON, returning the result as a string.
     * This throws a RuntimeException instead of a B2JsonException,
     * so use it carefully.
     */
    public static String toJsonOrThrowRuntime(Object obj) {
        return toJsonOrThrowRuntime(obj, B2JsonOptions.DEFAULT);
    }

    public static String toJsonOrThrowRuntime(Object obj, B2JsonOptions options) {
        try {
            return get().toJson(obj, options);
        } catch (B2JsonException e) {
            throw new IllegalArgumentException("failed to convert to json: " + e.getMessage(), e);
        }
    }

    /**
     * Parse an assumed JSON string into a defined class.
     * This throws a RuntimeException instead of a B2JsonException,
     * so use it carefully.
     * @param json JSON String to try and parse
     * @param clazz Class to map the JSON String to.
     * @param <T> The deserialized object casted to the specific type from clazz
     * @return the object deserialized from the JSON String
     */
    public static <T> T fromJsonOrThrowRuntime(String json, Class<T> clazz) {
        return fromJsonOrThrowRuntime(json, clazz, B2JsonOptions.DEFAULT);
    }

    public static <T> T fromJsonOrThrowRuntime(String json, Class<T> clazz, B2JsonOptions options) {
        try {
            return get().fromJson(json, clazz, options);
        } catch (B2JsonException e) {
            throw new IllegalArgumentException("failed to convert from json: " + e.getMessage(), e);
        }
    }


    /**
     * Turn a map into JSON, returning the result as a string.
     */
    public String mapToJson(Map<?, ?> map, Class<?> keyClass, Class<?> valueClass) throws B2JsonException {
        return mapToJson(map, keyClass, valueClass, B2JsonOptions.DEFAULT);
    }

    public String mapToJson(Map<?, ?> map, Class<?> keyClass, Class<?> valueClass, B2JsonOptions options) throws B2JsonException {
        if (map == null) {
            throw new B2JsonException("map must not be null");
        }
        final B2JsonTypeHandler keyHandler = handlerMap.getHandler(keyClass);
        final B2JsonTypeHandler valueHandler = handlerMap.getHandler(valueClass);
        final B2JsonTypeHandler handler = new B2JsonMapHandler(keyHandler, valueHandler);
        try (final B2JsonBoundedByteArrayOutputStream out = new B2JsonBoundedByteArrayOutputStream(SYSTEM_MAX_CAPACITY)) {
            B2JsonWriter jsonWriter = new B2JsonWriter(out, options);
            //noinspection unchecked
            handler.serialize(map, options, jsonWriter);
            return out.toString(B2StringUtil.UTF8);
        } catch (IOException e) {
            throw new RuntimeException("IO exception writing to string: " + e.getMessage());
        }
    }

    /**
     * Parses a JSON object into a map.
     */
    public <K, V> Map<K, V> mapFromJson(String json, Class<K> keyClass, Class<V> valueClass) throws B2JsonException {
        return mapFromJson(json, keyClass, valueClass, B2JsonOptions.DEFAULT);
    }

    public <K, V> Map<K, V> mapFromJson(String json, Class<K> keyClass, Class<V> valueClass, B2JsonOptions options) throws B2JsonException {
        final B2JsonTypeHandler keyHandler = handlerMap.getHandler(keyClass);
        final B2JsonTypeHandler valueHandler = handlerMap.getHandler(valueClass);
        final B2JsonTypeHandler handler = new B2JsonMapHandler(keyHandler, valueHandler);
        return fromJsonWithHandler(json, handler, options);
    }

    /**
     * Turn a map into JSON, returning the result as a string.
     */
    public String listToJson(List<?> list, Class<?> valueClass) throws B2JsonException {
        return listToJson(list, valueClass, B2JsonOptions.DEFAULT);
    }

    public String listToJson(List<?> list, Class<?> valueClass, B2JsonOptions options) throws B2JsonException {
        if (list == null) {
            throw new B2JsonException("list must not be null");
        }
        final B2JsonTypeHandler valueHandler = handlerMap.getHandler(valueClass);
        final B2JsonTypeHandler handler = new B2JsonListHandler(valueHandler);
        try (final B2JsonBoundedByteArrayOutputStream out = new B2JsonBoundedByteArrayOutputStream(SYSTEM_MAX_CAPACITY)) {
            B2JsonWriter jsonWriter = new B2JsonWriter(out, options);
            //noinspection unchecked
            handler.serialize(list, options, jsonWriter);
            return out.toString(B2StringUtil.UTF8);
        } catch (IOException e) {
            throw new RuntimeException("IO exception writing to string: " + e.getMessage());
        }
    }

    /**
     * Parses a JSON object into a map.
     */
    public <V> List<V> listFromJson(String json, Class<V> valueClass) throws B2JsonException {
        return listFromJson(json, valueClass, B2JsonOptions.DEFAULT);
    }

    public <V> List<V> listFromJson(String json, Class<V> valueClass, B2JsonOptions options) throws B2JsonException {
        final B2JsonTypeHandler valueHandler = handlerMap.getHandler(valueClass);
        final B2JsonTypeHandler handler = new B2JsonListHandler(valueHandler);
        return fromJsonWithHandler(json, handler, options);
    }

    /**
     * Parse JSON as an object of the given class with the given options,
     * reading from the input stream until reaching EOF.  Throws an error
     * if there is anything but whitespace after the JSON value.
     */
    public <T> T fromJsonUntilEof(InputStream in, Class<T> clazz) throws IOException, B2JsonException {
        return fromJsonUntilEof(in, clazz, B2JsonOptions.DEFAULT);
    }

    /**
     * Use the call that takes B2JsonOptions, no this one with 'int optionFlags'.
     */
    @Deprecated
    public <T> T fromJsonUntilEof(InputStream in, Class<T> clazz, int optionFlags) throws IOException, B2JsonException {
        return fromJsonUntilEof(in, clazz, optionsFromFlags(optionFlags));
    }

    public <T> T fromJsonUntilEof(InputStream in, Class<T> clazz, B2JsonOptions options) throws IOException, B2JsonException {
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
        return fromJson(in, clazz, B2JsonOptions.DEFAULT);
    }

    /**
     * Use the call that takes B2JsonOptions, no this one with 'int optionFlags'.
     */
    @Deprecated
    public <T> T fromJson(InputStream in, Class<T> clazz, int optionFlags) throws IOException, B2JsonException {
        return fromJson(in, clazz, optionsFromFlags(optionFlags));
    }

    /**
     * Parse the bytes from an InputStream as JSON using the supplied options, returning the parsed object.
     *
     * The Type parameter will usually be a class, which is straightforward to supply. However,
     * if you are trying to deserialize a parameterized type (like if obj is a
     * {@literal List<String>}, then you will need to supply a proper Type instance.
     *
     * Getting the Type can be done in at least two ways:
     * 1. If it is a member of an enclosing class, EnclosingClass.getDeclaredField(...).getGenericType()
     * 2. By constructing a class that implements Type.
     */
    public <T> T fromJson(InputStream in, Type type, B2JsonOptions options) throws IOException, B2JsonException {
        B2JsonReader reader = new B2JsonReader(new InputStreamReader(in, "UTF-8"));
        final B2JsonTypeHandler handler = handlerMap.getHandler(type);

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
        return fromJson(json, clazz, B2JsonOptions.DEFAULT);
    }

    /**
     * Use the call that takes B2JsonOptions, no this one with 'int optionFlags'.
     */
    @Deprecated
    public <T> T fromJson(String json, Class<T> clazz, int optionFlags) throws B2JsonException {
        return fromJson(json, clazz, optionsFromFlags(optionFlags));
    }

    public <T> T fromJson(String json, Class<T> clazz, B2JsonOptions options) throws B2JsonException {
        final B2JsonTypeHandler handler = handlerMap.getHandler(clazz);
        return fromJsonWithHandler(json, handler, options);
    }

    /**
     * Use the call that takes B2JsonOptions, no this one with 'int optionFlags'.
     */
    @Deprecated
    private <T> T fromJsonWithHandler(String json, B2JsonTypeHandler handler, int optionFlags) throws B2JsonException {
        return fromJsonWithHandler(json, handler, optionsFromFlags(optionFlags));
    }

    private <T> T fromJsonWithHandler(String json, B2JsonTypeHandler handler, B2JsonOptions options) throws B2JsonException {
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
        return fromJson(jsonUtf8Bytes, clazz, B2JsonOptions.DEFAULT);
    }

    /**
     * Use the call that takes B2JsonOptions, no this one with 'int optionFlags'.
     */
    @Deprecated
    public <T> T fromJson(byte[] jsonUtf8Bytes, Class<T> clazz, int optionFlags) throws IOException, B2JsonException {
        return fromJson(jsonUtf8Bytes, clazz, optionsFromFlags(optionFlags));
    }

    public <T> T fromJson(byte[] jsonUtf8Bytes, Class<T> clazz, B2JsonOptions options) throws IOException, B2JsonException {
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
        return fromUrlParameterMap(parameterMap, clazz, B2JsonOptions.DEFAULT);
    }

    /**
     * Use the call that takes B2JsonOptions, no this one with 'int optionFlags'.
     */
    @Deprecated
    public <T> T fromUrlParameterMap(Map<String, String> parameterMap, Class<T> clazz, int optionFlags) throws IOException, B2JsonException {
        return fromUrlParameterMap(parameterMap, clazz, optionsFromFlags(optionFlags));
    }

    public <T> T fromUrlParameterMap(Map<String, String> parameterMap, Class<T> clazz, B2JsonOptions options) throws IOException, B2JsonException {
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
     * <p>Class annotation that says a class is the base class for a union type.</p>
     *
     * <p>The class must not extend any other classes, but may implement interfaces.
     * It must have no B2Json field or constructor annotations</p>
     *
     * <p>Direct instances of this class cannot be (de)serialized.  Instances of
     * subclasses can be.</p>
     *
     * <p>For now, the implementation of deserialization is INEFFICIENT, so union
     * types should be used only for small objects.</p>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface union {
        String typeField();
    }

    /**
     * <p>Class annotation that applies to a class that is a @union.</p>
     *
     * <p>The value provided when de-serializing and the type is unknown.</p>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface defaultForUnknownType {
        String value();
    }

    /**
     * Field annotation that says a field is required to be present.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface required {}

    /**
     * Field annotation that says a field is optional.  The value will
     * always be included, even if it is null, when omitNull is false
     * (default); when omitNull is true and the field value is null,
     * the value will not be included. A B2JsonException is thrown
     * when omitNull is set to true on a primitive field; primitives
     * are not nullable objects so omitNull does not apply.
     * When deserializing, null/false/0 will be passed to
     * the constructor if the value is not present in the JSON.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface optional {
        boolean omitNull() default false;
    }

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
        boolean omitNull() default false;
    }

    /**
     * Field annotation that says the field is not included in JSON.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ignored {}

    /**
     * Annotation that says that a field exists in all versions at or after this one.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface firstVersion {
        int firstVersion();
    }

    /**
     * Annotation that says that a field exists in all versions in a range (inclusive).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface versionRange {
        int firstVersion();
        int lastVersion();
    }

    /**
     * Annotation that says this is a sensitive field and should be redacted when outputting
     * for logging
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface sensitive {}

    /**
     * Constructor annotation saying that this is the constructor B2Json
     * should use.  This constructor must take ALL of the serializable
     * fields as parameters.
     *
     * You must provide an "params" parameter that lists the order of
     * the parameters to the constructor.
     *
     * If present, the "discards" parameter is a comma-separated list of
     * field names which are allowed to be present in the parsed json,
     * but whose values will be discarded.  The names may be for fields
     * that don't exist or for fields marked @ignored.  This is useful
     * for accepting deprecated fields without having to use
     * ALLOW_EXTRA_FIELDS, which would accept ALL unknown fields.
     *
     * When versionParam is non-empty, it is the name of a parameter that
     * is not a field name, and will take the version number being constructed.
     * This should be included for objects that have multiple versions,
     * and the code in the constructor should validate the data based on it.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.CONSTRUCTOR)
    public @interface constructor {
        String params();
        String discards() default "";
        String versionParam() default "";
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

    /**
     * All of the B2Json annotations.
     */
    @SuppressWarnings("unchecked")
    /*package*/ static final Class<? extends Annotation>[] ALL_ANNOTATIONS =
            new Class[] {
                    union.class,
                    required.class,
                    optional.class,
                    optionalWithDefault.class,
                    ignored.class,
                    constructor.class,
                    defaultForInvalidEnumValue.class,
                    firstVersion.class,
                    versionRange.class
            };

    /**
     * Convert from deprecated options flags to options object.
     *
     * Called a lot, so optimized to always return the same objects.
     */
    private static B2JsonOptions optionsFromFlags(int optionFlags) {
        // There was only one option before we switched to B2JsonOptions, so
        // the logic is simple here.
        if ((optionFlags & B2Json.ALLOW_EXTRA_FIELDS) == 0) {
            return B2JsonOptions.DEFAULT;
        }
        else {
            return B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS;
        }
    }

}
