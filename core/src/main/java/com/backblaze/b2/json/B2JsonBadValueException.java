/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

/**
 * Exception thrown when a value in a JSON object is bad, even though it
 * is valid JSON.
 */
public class B2JsonBadValueException extends B2JsonException {

    public B2JsonBadValueException(String message) {
        super(message);
    }

    public B2JsonBadValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
