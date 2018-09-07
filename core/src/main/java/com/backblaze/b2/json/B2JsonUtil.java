/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.io.IOException;

public class B2JsonUtil {

    /**
     * Serialize an object that may be null.
     */
    public static <T> void serializeMaybeNull(B2JsonTypeHandler<T> handler, T obj, B2JsonWriter out, B2JsonOptions options) throws IOException, B2JsonException {
        if (obj == null) {
            out.writeText("null");
        }
        else {
            handler.serialize(obj, options, out);
        }
    }

    /**
     * Deserialize an object that may be null.
     */
    public static <T> T deserializeMaybeNull(B2JsonTypeHandler<T> handler, B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        if (in.peekNextNotWhitespaceChar() == 'n') {
            in.readNull();
            return null;
        }
        else {
            return handler.deserialize(in, options);
        }
    }
}
