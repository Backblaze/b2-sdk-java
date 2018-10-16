/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.util.EnumSet;


/**
 * Serialization for enum sets.
 */
public class B2JsonEnumSetHandler extends B2JsonNonUrlTypeHandler<EnumSet> {
    private final B2JsonTypeHandler itemHandler;

    public B2JsonEnumSetHandler(B2JsonTypeHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Class<EnumSet> getHandledClass() {
        return EnumSet.class;
    }

    public void serialize(EnumSet obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.startArray();
        for (Object item : obj) {
            out.startArrayValue();
            //noinspection unchecked
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out, options);
        }
        out.finishArray();
    }

    public EnumSet deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        EnumSet result = EnumSet.noneOf(itemHandler.getHandledClass());
        if (in.startArrayAndCheckForContents()) {
            do {
                //noinspection unchecked
                result.add(B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();
        return result;
    }

    public EnumSet defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
