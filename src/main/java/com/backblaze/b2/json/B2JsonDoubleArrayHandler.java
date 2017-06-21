/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class B2JsonDoubleArrayHandler extends B2JsonNonUrlTypeHandler<double[]> {

    private final B2JsonTypeHandler itemHandler;

    public B2JsonDoubleArrayHandler(B2JsonTypeHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Class<double[]> getHandledClass() {
        return double[].class;
    }

    public void serialize(double[] array, B2JsonWriter out) throws IOException, B2JsonException {
        out.setAllowNewlines(false);
        out.startArray();
        for (Double item : array) {
            out.startArrayValue();
            //noinspection unchecked
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out);
        }
        out.finishArray();
        out.setAllowNewlines(true);
    }

    public double[] deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
        List<Double> result = new ArrayList<>();
        if (in.startArrayAndCheckForContents()) {
            do {
                result.add((Double) B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();

        final int nElts = result.size();
        final double[] array = new double[nElts];
        int i = 0;
        for (Double elt : result) {
            if (elt == null) {
                throw new B2JsonBadValueException("can't put null in a double[].");
            }
            array[i] = elt;
            i++;
        }
        return array;
    }

    public double[] defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
