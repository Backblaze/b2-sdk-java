/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class B2JsonIntArrayHandler extends B2JsonNonUrlTypeHandler<int[]> {

    private final B2JsonTypeHandler itemHandler;

    public B2JsonIntArrayHandler(B2JsonTypeHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Class<int[]> getHandledClass() {
        return int[].class;
    }

    public void serialize(int[] array, B2JsonWriter out) throws IOException, B2JsonException {
        out.setAllowNewlines(false);
        out.startArray();
        for (int item : array) {
            out.startArrayValue();
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out);
        }
        out.finishArray();
        out.setAllowNewlines(true);
    }

    public int[] deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        List<Integer> result = new ArrayList<Integer>();
        if (in.startArrayAndCheckForContents()) {
            do {
                result.add((Integer) B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();

        final int nElts = result.size();
        final int[] array = new int[nElts];
        int i = 0;
        for (Integer elt : result) {
            if (elt == null) {
                throw new B2JsonBadValueException("can't put null in a int[].");
            }
            array[i] = elt;
            i++;
        }
        return array;
    }

    public int[] defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
