/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;

/**
 * (De)serializes Long objects.
 */
public class B2JsonLongHandler implements B2JsonTypeHandler<Long> {

    private final boolean isPrimitive;

    public B2JsonLongHandler(boolean isPrimitive) {
        this.isPrimitive = isPrimitive;
    }

    public Class<Long> getHandledClass() {
        return Long.class;
    }

    public void serialize(Long obj, B2JsonWriter out) throws IOException {
        out.writeText(obj.toString());
    }

    public Long deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
        String str = in.readNumberAsString();
        return deserializeUrlParam(str);
    }

    public Long deserializeUrlParam(String value) throws B2JsonException {
        try {
            return Long.valueOf(value);
        }
        catch (NumberFormatException e) {
            throw new B2JsonException("bad long: " + value);
        }
    }

    public Long defaultValueForOptional() {
        if (isPrimitive) {
            return 0L;
        }
        else {
            return null;
        }
    }

    public boolean isStringInJson() {
        return false;
    }
}
