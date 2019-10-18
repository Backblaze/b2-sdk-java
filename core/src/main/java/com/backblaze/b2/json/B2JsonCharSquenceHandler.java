/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;

/**
 * (De)serializes CharSequence objects
 *
 * When deserializing, the underlying concrete type is String and thus will allocate even
 * when the JSON input is a string.
 */
public class B2JsonCharSquenceHandler implements B2JsonTypeHandler<CharSequence> {

    @Override
    public Class<CharSequence> getHandledClass() {
        return CharSequence.class;
    }

    @Override
    public void serialize(CharSequence obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.writeString(obj);
    }

    @Override
    public CharSequence deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        return in.readString();
    }

    @Override
    public CharSequence deserializeUrlParam(String urlValue) throws B2JsonException {
        return urlValue;
    }

    @Override
    public CharSequence defaultValueForOptional() {
        return null;
    }

    @Override
    public boolean isStringInJson() {
        return true;
    }
}
