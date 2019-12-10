/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Interface for (de)serializing one class of object.
 */
public interface B2JsonTypeHandler<T> {

    /**
     * What class does this handle?
     */
    Class<T> getHandledClass();

    default Type getHandledType() {
        return getHandledClass();
    }

    /**
     * Serialize one object of the class to a JSON output stream.
     *
     * The object is guaranteed not to be null.
     */
    void serialize(T obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException;

    /**
     * Deserialize one object from a JSON input stream.
     *
     * Will never be called when there is "null" in the input stream,
     * and will never return null.
     */
    T deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException;

    /**
     * When an API is called with GET, this is used to deserialize one of the
     * values.
     */
    T deserializeUrlParam(String urlValue) throws B2JsonException;

    /**
     * Returns the default value to use when an optional field is not present.
     */
    T defaultValueForOptional();

    /**
     * Does this type look like a string in JSON?
     */
    boolean isStringInJson();
}
