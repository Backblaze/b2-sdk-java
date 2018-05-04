/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

public class B2TooManyRequestsException extends B2Exception {
    public static final String DEFAULT_CODE = "too_many_requests";
    public static final int STATUS = 429;


    public B2TooManyRequestsException(String code, Integer retryAfterSecondsOrNull, String message) {
        this(code, retryAfterSecondsOrNull, message, null);
    }

    public B2TooManyRequestsException(String code,
                                      Integer retryAfterSecondsOrNull,
                                      String message,
                                      Throwable cause) {
        super(orIfNull(code, DEFAULT_CODE), STATUS, retryAfterSecondsOrNull, message, cause);
    }
}
