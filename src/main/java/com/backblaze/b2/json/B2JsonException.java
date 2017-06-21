/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

/**
 * Exception thrown when there is a problem reading or writing JSON.
 */
public class B2JsonException extends Exception {

    public B2JsonException(String message) {
        super(message);
    }
    public B2JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
