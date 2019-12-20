/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.TreeSet;

public class B2JsonTreeSetHandler extends B2JsonNonUrlTypeHandler<TreeSet> {
    private final B2JsonTypeHandler itemHandler;

    public B2JsonTreeSetHandler(B2JsonTypeHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Type getHandledType() {
        return new B2TypeResolver.ResolvedParameterizedType(
                TreeSet.class,
                new Type[]{itemHandler.getHandledType()});
    }

    public void serialize(TreeSet obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.startArray();
        for (Object item : obj) {
            out.startArrayValue();
            //noinspection unchecked
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out, options);
        }
        out.finishArray();
    }

    public TreeSet deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        TreeSet result = new TreeSet();
        if (in.startArrayAndCheckForContents()) {
            do {
                //noinspection unchecked
                result.add(B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();
        return result;
    }

    public TreeSet defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
