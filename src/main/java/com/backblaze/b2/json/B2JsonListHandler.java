/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class B2JsonListHandler extends B2JsonNonUrlTypeHandler<List> {

    private static final Set<Class<?>> PRIMITIVE_TYPES;
    static {
        PRIMITIVE_TYPES = new HashSet<>();
        PRIMITIVE_TYPES.add(Boolean.class);
        PRIMITIVE_TYPES.add(Byte.class);
        PRIMITIVE_TYPES.add(Character.class);
        PRIMITIVE_TYPES.add(Double.class);
        PRIMITIVE_TYPES.add(Float.class);
        PRIMITIVE_TYPES.add(Integer.class);
        PRIMITIVE_TYPES.add(Long.class);
    }

    private final B2JsonTypeHandler itemHandler;

    public B2JsonListHandler(B2JsonTypeHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    public Class<List> getHandledClass() {
        return List.class;
    }

    public void serialize(List obj, B2JsonWriter out) throws IOException, B2JsonException {
        if (obj.size() != 0 && PRIMITIVE_TYPES.contains(obj.get(0).getClass())) {
            out.setAllowNewlines(false);
        }
        out.startArray();
        for (Object item : obj) {
            out.startArrayValue();
            //noinspection unchecked
            B2JsonUtil.serializeMaybeNull(itemHandler, item, out);
        }
        out.finishArray();
        out.setAllowNewlines(true);
    }

    public List deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
        List result = new ArrayList();
        if (in.startArrayAndCheckForContents()) {
            do {
                //noinspection unchecked
                result.add(B2JsonUtil.deserializeMaybeNull(itemHandler, in, options));
            } while (in.arrayHasMoreValues());
        }
        in.finishArray();
        return result;
    }

    public List defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
