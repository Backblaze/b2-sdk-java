/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class B2JsonObjectArrayHandler<T> extends B2JsonNonUrlTypeHandler<T> {

    private final B2JsonTypeHandler<Object> itemHandler;
    private final Class<T> arrayClazz;
    private final Class eltClazz;

    public B2JsonObjectArrayHandler(Class<T> arrayClazz, Class eltClazz, B2JsonTypeHandler itemHandler) {
        this.arrayClazz = arrayClazz;
        this.eltClazz = eltClazz;
        //noinspection unchecked
        this.itemHandler = itemHandler;
    }

    public Class<T> getHandledClass() {
        return arrayClazz;
    }

    public void serialize(T array, B2JsonWriter out) throws IOException, B2JsonException {
        out.startArray();
        final int nElts = Array.getLength(array);
        for (int i=0; i < nElts; i++) {
            out.startArrayValue();
            B2JsonUtil.serializeMaybeNull(itemHandler, Array.get(array,i), out);
        }
        out.finishArray();
    }

    public T deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        List<Object> result = new ArrayList<>();
        if (in.startArrayAndCheckForContents()) {
            do {
                result.add(B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();

        final int nElts = result.size();
        final Object array = Array.newInstance(eltClazz, nElts);
        int i = 0;
        for (Object elt : result) {
            //noinspection unchecked
            Array.set(array, i, elt);
            i++;
        }
        //noinspection unchecked
        return (T) array;
    }

    public T defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
