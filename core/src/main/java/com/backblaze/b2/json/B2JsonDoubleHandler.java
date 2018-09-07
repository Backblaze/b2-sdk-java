/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;

/**
 * (De)serializes Double objects.
 */
public class B2JsonDoubleHandler implements B2JsonTypeHandler<Double> {

    private final boolean isPrimitive;

    public B2JsonDoubleHandler(boolean isPrimitive) {
        this.isPrimitive = isPrimitive;
    }

    public Class<Double> getHandledClass() {
        return Double.class;
    }

    public void serialize(Double obj, B2JsonOptions options, B2JsonWriter out) throws IOException {
        out.writeText(obj.toString());
    }

    public Double deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        String str = in.readNumberAsString();
        return deserializeUrlParam(str);
    }

    public Double deserializeUrlParam(String str) throws B2JsonException {
        try {
            return Double.valueOf(str);
        }
        catch (NumberFormatException e) {
            throw new B2JsonException("bad Double: " + str);
        }
    }

    public Double defaultValueForOptional() {
        if (isPrimitive) {
            return 0.0;
        }
        else {
            return null;
        }
    }

    public boolean isStringInJson() {
        return false;
    }
}
