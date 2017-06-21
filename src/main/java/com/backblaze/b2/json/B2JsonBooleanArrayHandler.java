/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class B2JsonBooleanArrayHandler extends B2JsonNonUrlTypeHandler<boolean[]> {

    private final B2JsonTypeHandler<Boolean> itemHandler;

    public B2JsonBooleanArrayHandler(B2JsonTypeHandler itemHandler) {
        //noinspection unchecked
        this.itemHandler = itemHandler;
    }

    public Class<boolean[]> getHandledClass() {
        return boolean[].class;
    }

    public void serialize(boolean[] array, B2JsonWriter out) throws IOException, B2JsonException {
        out.setAllowNewlines(false);
        out.startArray();
        for (Boolean item : array) {
            out.startArrayValue();
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out);
        }
        out.finishArray();
        out.setAllowNewlines(true);
    }

    public boolean[] deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
        List<Boolean> result = new ArrayList<>();
        if (in.startArrayAndCheckForContents()) {
            do {
                result.add(B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();

        final int nElts = result.size();
        final boolean[] array = new boolean[nElts];
        int i = 0;
        for (Boolean elt : result) {
            if (elt == null) {
                throw new B2JsonBadValueException("can't put null in a boolean[].");
            }
            array[i] = elt;
            i++;
        }
        return array;
    }

    public boolean[] defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
