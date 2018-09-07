/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class B2JsonFloatArrayHandler extends B2JsonNonUrlTypeHandler<float[]> {

    private final B2JsonTypeHandler itemHandler;

    public B2JsonFloatArrayHandler(B2JsonTypeHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Class<float[]> getHandledClass() {
        return float[].class;
    }

    public void serialize(float[] array, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.setAllowNewlines(false);
        out.startArray();
        for (Float item : array) {
            out.startArrayValue();
            //noinspection unchecked
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out, options);
        }
        out.finishArray();
        out.setAllowNewlines(true);
    }

    public float[] deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        List<Float> result = new ArrayList<>();
        if (in.startArrayAndCheckForContents()) {
            do {
                result.add((Float) B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();

        final int nElts = result.size();
        final float[] array = new float[nElts];
        int i = 0;
        for (Float elt : result) {
            if (elt == null) {
                throw new B2JsonBadValueException("can't put null in a float[].");
            }
            array[i] = elt;
            i++;
        }
        return array;
    }

    public float[] defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
