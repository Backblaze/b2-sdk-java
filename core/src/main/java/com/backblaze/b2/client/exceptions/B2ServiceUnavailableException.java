/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

public class B2ServiceUnavailableException extends B2Exception {
    public static final String DEFAULT_CODE = "service_unavailable";
    public static final int STATUS = 503;

    public B2ServiceUnavailableException(String code,
                                         Integer retryAfterSecondsOrNull,
                                         String message) {
        this(code, retryAfterSecondsOrNull, message, null);
    }

    public B2ServiceUnavailableException(String code,
                                         Integer retryAfterSecondsOrNull,
                                         String message,
                                         Throwable cause) {
        super(orIfNull(code, DEFAULT_CODE), STATUS, retryAfterSecondsOrNull, message, cause);
    }
}
