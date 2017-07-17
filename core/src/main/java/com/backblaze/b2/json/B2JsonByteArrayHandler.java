/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class B2JsonByteArrayHandler extends B2JsonNonUrlTypeHandler<byte[]> {

    private final B2JsonTypeHandler itemHandler;

    public B2JsonByteArrayHandler(B2JsonTypeHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Class<byte[]> getHandledClass() {
        return byte[].class;
    }

    public void serialize(byte[] array, B2JsonWriter out) throws IOException, B2JsonException {
        out.setAllowNewlines(false);
        out.startArray();
        for (Byte item : array) {
            out.startArrayValue();
            //noinspection unchecked
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out);
        }
        out.finishArray();
        out.setAllowNewlines(true);
    }

    public byte[] deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
        List<Byte> result = new ArrayList<>();
        if (in.startArrayAndCheckForContents()) {
            do {
                result.add((Byte) B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();

        final int nElts = result.size();
        final byte[] array = new byte[nElts];
        int i = 0;
        for (Byte elt : result) {
            if (elt == null) {
                throw new B2JsonBadValueException("can't put null in a byte[].");
            }
            array[i] = elt;
            i++;
        }
        return array;
    }

    public byte[] defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
