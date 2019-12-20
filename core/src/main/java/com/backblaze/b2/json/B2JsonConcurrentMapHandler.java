/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class B2JsonConcurrentMapHandler extends B2JsonNonUrlTypeHandler<ConcurrentMap> {

    private final B2JsonTypeHandler keyHandler;
    private final B2JsonTypeHandler valueHandler;

    public B2JsonConcurrentMapHandler(B2JsonTypeHandler keyHandler, B2JsonTypeHandler valueHandler) throws B2JsonException {
        if (!keyHandler.isStringInJson()) {
            throw new B2JsonException("Map key is not a string: " + keyHandler.getHandledType());
        }
        this.keyHandler = keyHandler;
        this.valueHandler = valueHandler;
    }

    public Type getHandledType() {
        return new B2TypeResolver.ResolvedParameterizedType(
                ConcurrentMap.class,
                new Type[] {keyHandler.getHandledType(), valueHandler.getHandledType()});
    }

    public void serialize(ConcurrentMap obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.startObject();
        for (Map.Entry entry : (Set<Map.Entry>) obj.entrySet()) {
            out.startObjectFieldName();
            keyHandler.serialize(entry.getKey(), options, out);
            out.writeText(": ");
            B2JsonUtil.serializeMaybeNull(valueHandler, entry.getValue(), out, options);
        }
        out.finishObject();
    }

    public ConcurrentMap deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        ConcurrentMap result = new ConcurrentHashMap();
        if (in.startObjectAndCheckForContents()) {
            do {
                Object key = keyHandler.deserialize(in, options);
                in.skipObjectColon();
                Object value = B2JsonUtil.deserializeMaybeNull(valueHandler, in, options);
                result.put(key, value);
            } while (in.objectHasMoreFields());
        }
        in.finishObject();
        return result;
    }

    public ConcurrentMap defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
