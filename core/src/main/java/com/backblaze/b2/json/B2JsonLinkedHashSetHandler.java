/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.util.LinkedHashSet;

public class B2JsonLinkedHashSetHandler extends B2JsonNonUrlTypeHandler<LinkedHashSet> {
    private final B2JsonTypeHandler itemHandler;

    public B2JsonLinkedHashSetHandler(B2JsonTypeHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Class<LinkedHashSet> getHandledClass() {
        return LinkedHashSet.class;
    }

    public void serialize(LinkedHashSet obj, B2JsonWriter out) throws IOException, B2JsonException {
        out.startArray();
        for (Object item : obj) {
            out.startArrayValue();
            //noinspection unchecked
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out);
        }
        out.finishArray();
    }

    public LinkedHashSet deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        LinkedHashSet result = new LinkedHashSet();
        if (in.startArrayAndCheckForContents()) {
            do {
                //noinspection unchecked
                result.add(B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();
        return result;
    }

    public LinkedHashSet defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
