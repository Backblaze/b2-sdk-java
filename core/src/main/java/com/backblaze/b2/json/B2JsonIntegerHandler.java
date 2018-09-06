/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;

/**
 * (De)serializes Integer objects.
 */
public class B2JsonIntegerHandler implements B2JsonTypeHandler<Integer> {

    private final boolean isPrimitive;

    public B2JsonIntegerHandler(boolean isPrimitive) {
        this.isPrimitive = isPrimitive;
    }

    public Class<Integer> getHandledClass() {
        return Integer.class;
    }

    public void serialize(Integer obj, B2JsonWriter out) throws IOException {
        out.writeText(obj.toString());
    }

    public Integer deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        String str = in.readNumberAsString();
        return deserializeUrlParam(str);
    }

    public Integer deserializeUrlParam(String value) throws B2JsonException {
        try {
            return Integer.valueOf(value);
        }
        catch (NumberFormatException e) {
            throw new B2JsonException("bad integer: " + value);
        }
    }

    public Integer defaultValueForOptional() {
        if (isPrimitive) {
            return 0;
        }
        else {
            return null;
        }
    }

    public boolean isStringInJson() {
        return false;
    }
}
