/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

public class B2RequestTimeoutException extends B2Exception {
    static final int STATUS = 408;

    public B2RequestTimeoutException(String code,
                                     Integer retryAfterSecondsOrNull,
                                     String message) {
        this(code, retryAfterSecondsOrNull, message, null);
    }

    public B2RequestTimeoutException(String code,
                                     Integer retryAfterSecondsOrNull,
                                     String message,
                                     Throwable cause) {
        super(code, STATUS, retryAfterSecondsOrNull, message, cause);
    }
}
