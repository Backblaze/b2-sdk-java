/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class B2JsonLongArrayHandler extends B2JsonNonUrlTypeHandler<long[]> {

    private final B2JsonTypeHandler itemHandler;

    public B2JsonLongArrayHandler(B2JsonTypeHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Class<long[]> getHandledClass() {
        return long[].class;
    }

    public void serialize(long[] array, B2JsonWriter out) throws IOException, B2JsonException {
        out.setAllowNewlines(false);
        out.startArray();
        for (Long item : array) {
            out.startArrayValue();
            //noinspection unchecked
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out);
        }
        out.finishArray();
        out.setAllowNewlines(true);
    }

    public long[] deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
        List<Long> result = new ArrayList<>();
        if (in.startArrayAndCheckForContents()) {
            do {
                result.add((Long) B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();

        final int nElts = result.size();
        final long[] array = new long[nElts];
        int i = 0;
        for (Long elt : result) {
            if (elt == null) {
                throw new B2JsonBadValueException("can't put null in a long[].");
            }
            array[i] = elt;
            i++;
        }
        return array;
    }

    public long[] defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
