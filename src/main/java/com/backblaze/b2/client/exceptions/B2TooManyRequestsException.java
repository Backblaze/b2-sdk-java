/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

public class B2TooManyRequestsException extends B2Exception {
    static final int STATUS = 429;


    public B2TooManyRequestsException(String code, Integer retryAfterSecondsOrNull, String message) {
        this(code, retryAfterSecondsOrNull, message, null);
    }

    public B2TooManyRequestsException(String code,
                                      Integer retryAfterSecondsOrNull,
                                      String message,
                                      Throwable cause) {
        super(code, STATUS, retryAfterSecondsOrNull, message, cause);
    }
}
