/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * (De)serializes Character objects.
 */
public class B2JsonCharacterHandler implements B2JsonTypeHandler<Character> {

    private final boolean isPrimitive;

    public B2JsonCharacterHandler(boolean isPrimitive) {
        this.isPrimitive = isPrimitive;
    }

    public Type getHandledType() {
        return Character.class;
    }

    public void serialize(Character obj, B2JsonOptions options, B2JsonWriter out) throws IOException {
        out.writeText(Integer.toString((int) obj.charValue()));
    }

    public Character deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        String str = in.readNumberAsString();
        try {
            int value = Integer.valueOf(str);
            if (value < 0 || 0xFFFF < value) {
                throw new B2JsonException("char value out of range: " + str);
            }
            return (char) value;
        }
        catch (NumberFormatException e) {
            throw new B2JsonException("bad character: " + str);
        }
    }

    public Character deserializeUrlParam(String urlValue) throws B2JsonException {
        try {
            int value = Integer.valueOf(urlValue);
            if (value < 0 || 0xFFFF < value) {
                throw new B2JsonException("char value out of range: " + urlValue);
            }
            return (char) value;
        }
        catch (NumberFormatException e) {
            throw new B2JsonException("bad character: " + urlValue);
        }
    }

    public Character defaultValueForOptional() {
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
