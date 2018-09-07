/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class B2JsonMapHandler extends B2JsonNonUrlTypeHandler<Map> {

    private final B2JsonTypeHandler keyHandler;
    private final B2JsonTypeHandler valueHandler;

    public B2JsonMapHandler(B2JsonTypeHandler keyHandler, B2JsonTypeHandler valueHandler) throws B2JsonException {
        if (!keyHandler.isStringInJson()) {
            throw new B2JsonException("Map key is not a string: " + keyHandler.getHandledClass());
        }
        this.keyHandler = keyHandler;
        this.valueHandler = valueHandler;
    }

    public Class<Map> getHandledClass() {
        return Map.class;
    }

    public void serialize(Map obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.startObject();
        //noinspection unchecked
        for (Map.Entry entry : (Set<Map.Entry>) obj.entrySet()) {
            // Some maps allow key to be null. That's not legal in json though, so don't allow it.
            if (entry.getKey() == null) {
                throw new B2JsonException("Map key is null");

            }
            out.startObjectFieldName();
            //noinspection unchecked
            keyHandler.serialize(entry.getKey(), options, out);
            out.writeText(": ");
            //noinspection unchecked
            B2JsonUtil.serializeMaybeNull(valueHandler, entry.getValue(), out, options);
        }
        out.finishObject();
    }

    public Map deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        Map result = new TreeMap();
        if (in.startObjectAndCheckForContents()) {
            do {
                Object key = keyHandler.deserialize(in, options);
                in.skipObjectColon();
                Object value = B2JsonUtil.deserializeMaybeNull(valueHandler, in, options);
                //noinspection unchecked
                result.put(key, value);
            } while (in.objectHasMoreFields());
        }
        in.finishObject();
        return result;
    }

    public Map defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
