/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

/**
 * Base class for handlers that don't support reading URL parameters.
 */
public abstract class B2JsonNonUrlTypeHandler<T> extends B2JsonInitializedTypeHandler<T> {

    public T deserializeUrlParam(String urlValue) throws B2JsonException {
        throw new B2JsonException("type not supported in URL parameter");
    }
}
