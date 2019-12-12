/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * (De)serializes Float objects.
 */
public class B2JsonFloatHandler implements B2JsonTypeHandler<Float> {

    private final boolean isPrimitive;

    public B2JsonFloatHandler(boolean isPrimitive) {
        this.isPrimitive = isPrimitive;
    }

    public Type getHandledType() {
        return Float.class;
    }

    public void serialize(Float obj, B2JsonOptions options, B2JsonWriter out) throws IOException {
        out.writeText(obj.toString());
    }

    public Float deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        String str = in.readNumberAsString();
        return deserializeUrlParam(str);
    }

    public Float deserializeUrlParam(String urlValue) throws B2JsonException {
        try {
            return Float.valueOf(urlValue);
        }
        catch (NumberFormatException e) {
            throw new B2JsonException("bad float: " + urlValue);
        }
    }

    public Float defaultValueForOptional() {
        if (isPrimitive) {
            return 0.0f;
        }
        else {
            return null;
        }
    }

    public boolean isStringInJson() {
        return false;
    }
}
