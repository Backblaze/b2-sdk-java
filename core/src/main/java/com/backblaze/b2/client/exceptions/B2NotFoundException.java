/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

public class B2NotFoundException extends B2Exception {
    static final String CODE = "not_found";
    static final int STATUS = 404;

    public B2NotFoundException(Integer retryAfterSecondsOrNull,
                               String message) {
        this(CODE, retryAfterSecondsOrNull, message);
    }

    public B2NotFoundException(String code,
                               Integer retryAfterSecondsOrNull,
                               String message) {
        this(code, retryAfterSecondsOrNull, message, null);
    }

    public B2NotFoundException(String code,
                               Integer retryAfterSecondsOrNull,
                               String message,
                               Throwable cause) {
        super(code, STATUS, retryAfterSecondsOrNull, message, cause);
    }
}
