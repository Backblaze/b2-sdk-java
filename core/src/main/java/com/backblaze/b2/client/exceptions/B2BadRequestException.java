/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

public class B2BadRequestException extends B2Exception {
    public static final int STATUS = 400;

    public B2BadRequestException(String code,
                                 Integer retryAfterSecondsOrNull,
                                 String message) {
        this(code, retryAfterSecondsOrNull, message, null);
    }

    public B2BadRequestException(String code,
                                 Integer retryAfterSecondsOrNull,
                                 String message,
                                 Throwable cause) {
        super(code, STATUS, retryAfterSecondsOrNull, message, cause);
    }
}
