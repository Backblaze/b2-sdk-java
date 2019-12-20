/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class B2JsonCharArrayHandler extends B2JsonNonUrlTypeHandler<char[]> {

    private final B2JsonTypeHandler itemHandler;

    public B2JsonCharArrayHandler(B2JsonTypeHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Type getHandledType() {
        return char[].class;
    }

    public void serialize(char[] array, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.setAllowNewlines(false);
        out.startArray();
        for (char item : array) {
            out.startArrayValue();
            //noinspection unchecked
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out, options);
        }
        out.finishArray();
        out.setAllowNewlines(true);
    }

    public char[] deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        List<Character> result = new ArrayList<>();
        if (in.startArrayAndCheckForContents()) {
            do {
                result.add((Character) B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();

        final int nElts = result.size();
        final char[] array = new char[nElts];
        int i = 0;
        for (Character elt : result) {
            if (elt == null) {
                throw new B2JsonBadValueException("can't put null in a char[].");
            }
            array[i] = elt;
            i++;
        }
        return array;
    }

    public char[] defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
