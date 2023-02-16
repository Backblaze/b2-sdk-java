/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLongArray;

public class B2JsonAtomicLongArrayHandler extends B2JsonNonUrlTypeHandler<AtomicLongArray> {

    private final B2JsonTypeHandler<Long> itemHandler;

    public B2JsonAtomicLongArrayHandler(B2JsonTypeHandler<Long> itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Type getHandledType() {
        return AtomicLongArray.class;
    }

    public void serialize(AtomicLongArray array,
                          B2JsonOptions options,
                          B2JsonWriter out) throws IOException, B2JsonException {
        out.setAllowNewlines(false);
        out.startArray();
        for (int i = 0; i<array.length(); i++) {
            final Long item = array.get(i);
            out.startArrayValue();
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out, options);
        }
        out.finishArray();
        out.setAllowNewlines(true);
    }

    public AtomicLongArray deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        final List<Long> result = new ArrayList<>();
        if (in.startArrayAndCheckForContents()) {
            do {
                result.add(B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();

        final int nElts = result.size();
        final AtomicLongArray array = new AtomicLongArray(nElts);
        int i = 0;
        for (Long elt : result) {
            if (elt == null) {
                throw new B2JsonBadValueException("can't put null in an AtomicLongArray.");
            }
            array.set(i, elt);
            i++;
        }
        return array;
    }

    public AtomicLongArray defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
