/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;

/**
 * (De)serializes Float objects.
 */
public class B2JsonFloatHandler implements B2JsonTypeHandler<Float> {

    private final boolean isPrimitive;

    public B2JsonFloatHandler(boolean isPrimitive) {
        this.isPrimitive = isPrimitive;
    }

    public Class<Float> getHandledClass() {
        return Float.class;
    }

    public void serialize(Float obj, B2JsonWriter out) throws IOException {
        out.writeText(obj.toString());
    }

    public Float deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
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
