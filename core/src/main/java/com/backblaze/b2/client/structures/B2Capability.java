/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2JsonException;
import com.backblaze.b2.json.B2JsonReader;
import com.backblaze.b2.json.B2JsonTypeHandler;
import com.backblaze.b2.json.B2JsonWriter;
import com.backblaze.b2.util.B2StringUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public enum B2Capability {

    LIST_KEYS,
    WRITE_KEYS,
    DELETE_KEYS,

    LIST_BUCKETS,
    WRITE_BUCKETS,
    DELETE_BUCKETS,

    LIST_FILES,
    READ_FILES,
    SHARE_FILES,
    WRITE_FILES,
    DELETE_FILES;

    /**
     * The camel-case name used in the API.
     */
    private final String camelCaseName;

    /**
     * Map from camel-case name to value.
     */
    private static final Map<String, B2Capability> camelCaseNameToValue = makeCamelCaseNameToValue();

    B2Capability() {
        camelCaseName = B2StringUtil.underscoresToCamelCase(this.toString(), false);
    }

    public static B2Capability fromCamelCaseName(String camelCaseName) {
        B2Capability result = camelCaseNameToValue.get(camelCaseName);
        if (result == null) {
            throw new IllegalArgumentException("not a valid camel case capability: " + camelCaseName);
        }
        return result;
    }


    public String getCamelCaseName() {
        return camelCaseName;
    }

    private static Map<String, B2Capability> makeCamelCaseNameToValue() {
        final Map<String, B2Capability> result = new HashMap<>();
        for (B2Capability capability : values()) {
            result.put(capability.getCamelCaseName(), capability);
        }
        return result;
    }

    @SuppressWarnings("unused") // used by reflection
    private static B2JsonTypeHandler<B2Capability> getJsonTypeHandler() {
        return new B2Capability.JsonHandler();
    }

    public static class JsonHandler implements B2JsonTypeHandler<B2Capability> {

        @Override
        public Class<B2Capability> getHandledClass() {
            return B2Capability.class;
        }

        @Override
        public void serialize(B2Capability obj, B2JsonWriter out) throws IOException {
            out.writeString(obj.getCamelCaseName());
        }

        @Override
        public B2Capability deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
            return B2Capability.fromCamelCaseName(in.readString());
        }

        @Override
        public B2Capability deserializeUrlParam(String urlValue) {
            return B2Capability.fromCamelCaseName(urlValue);
        }

        @Override
        public B2Capability defaultValueForOptional() {
            return null;
        }

        @Override
        public boolean isStringInJson() {
            return true;
        }
    }


}
