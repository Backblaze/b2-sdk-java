/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;

/**
 * (De)serializes Boolean objects.
 */
public class B2JsonBooleanHandler implements B2JsonTypeHandler<Boolean> {

    private final boolean isPrimitive;

    public B2JsonBooleanHandler(boolean isPrimitive) {
        this.isPrimitive = isPrimitive;
    }

    public Class<Boolean> getHandledClass() {
        return Boolean.class;
    }

    public void serialize(Boolean obj, B2JsonOptions options, B2JsonWriter out) throws IOException {
        out.writeText(obj.toString());
    }

    public Boolean deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        char c = in.peekNextNotWhitespaceChar();
        if (c == 'f') {
            in.readFalse();
            return false;
        }
        else if (c == 't') {
            in.readTrue();
            return true;
        }
        else {
            throw new B2JsonException("expected boolean, but next char was " + c);
        }
    }

    public Boolean deserializeUrlParam(String urlValue) throws B2JsonException {
        if ("true".equals(urlValue)) {
            return true;
        }
        else if ("false".equals(urlValue)) {
            return false;
        }
        else {
            throw new B2JsonException("expected boolean, but value was " + urlValue);
        }
    }

    public Boolean defaultValueForOptional() {
        if (isPrimitive) {
            return false;
        }
        else {
            return null;
        }
    }

    public boolean isStringInJson() {
        return false;
    }
}
