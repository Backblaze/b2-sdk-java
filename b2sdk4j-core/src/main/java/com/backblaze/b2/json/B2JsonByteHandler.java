/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;

/**
 * (De)serializes Byte objects.
 */
public class B2JsonByteHandler implements B2JsonTypeHandler<Byte> {

    private final boolean isPrimitive;

    public B2JsonByteHandler(boolean isPrimitive) {
        this.isPrimitive = isPrimitive;
    }

    public Class<Byte> getHandledClass() {
        return Byte.class;
    }

    public void serialize(Byte obj, B2JsonWriter out) throws IOException {
        out.writeText(obj.toString());
    }

    public Byte deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
        String str = in.readNumberAsString();
        try {
            return Byte.valueOf(str);
        }
        catch (NumberFormatException e) {
            throw new B2JsonException("bad byte: " + str);
        }
    }

    public Byte deserializeUrlParam(String urlValue) throws B2JsonException {
        try {
            return Byte.valueOf(urlValue);
        }
        catch (NumberFormatException e) {
            throw new B2JsonException("bad byte: " + urlValue);
        }
    }

    public Byte defaultValueForOptional() {
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
