/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Serialize and deserialize sets
 */
public class B2JsonSetHandler extends B2JsonNonUrlTypeHandler<Set> {
    private final B2JsonTypeHandler itemHandler;

    public B2JsonSetHandler(B2JsonTypeHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Class<Set> getHandledClass() {
        return Set.class;
    }

    public void serialize(Set obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.startArray();
        for (Object item : obj) {
            out.startArrayValue();
            //noinspection unchecked
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out, options);
        }
        out.finishArray();
    }

    public Set deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        Set result = new HashSet();
        if (in.startArrayAndCheckForContents()) {
            do {
                //noinspection unchecked
                result.add(B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();
        return result;
    }

    public Set defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
