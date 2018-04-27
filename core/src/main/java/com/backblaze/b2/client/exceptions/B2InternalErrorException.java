/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

public class B2InternalErrorException extends B2Exception {
    public static final String DEFAULT_CODE = "internal_error";
    public static final int STATUS = 500;

    public B2InternalErrorException(String message) {
        this(DEFAULT_CODE, message);
    }
    public B2InternalErrorException(String code,
                                    String message) {
        this(code, null, message, null);
    }

    public B2InternalErrorException(String code,
                                    Integer retryAfterSecondsOrNull,
                                    String message) {
        this(code, retryAfterSecondsOrNull, message, null);
    }

    public B2InternalErrorException(String code,
                                    Integer retryAfterSecondsOrNull,
                                    String message,
                                    Throwable cause) {
        super(orIfNull(code, DEFAULT_CODE), STATUS, retryAfterSecondsOrNull, message, cause);
    }
}
