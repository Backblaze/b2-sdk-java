/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;

/**
 * (De)serializes String objects.
 */
public class B2JsonStringHandler implements B2JsonTypeHandler<String> {

    public Class<String> getHandledClass() {
        return String.class;
    }

    public void serialize(String obj, B2JsonWriter out) throws IOException {
        out.writeString(obj);
    }

    public String deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        return in.readString();
    }

    public String deserializeUrlParam(String urlValue) throws B2JsonException {
        return urlValue;
    }

    public String defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return true;
    }
}
