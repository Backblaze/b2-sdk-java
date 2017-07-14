/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

/**
 * B2ConnectionBrokenException represents the network connection to
 * the B2 servers having been closed unexpectedly.
 *
 * Note that this exception is generated locally by the SDK and
 * has a non-standard STATUS code.
 */
public class B2ConnectionBrokenException extends B2NetworkBaseException {
    static final int STATUS = 902;

    public B2ConnectionBrokenException(String code,
                                       Integer retryAfterSecondsOrNull,
                                       String message) {
        this(code, retryAfterSecondsOrNull, message, null);
    }

    public B2ConnectionBrokenException(String code,
                                       Integer retryAfterSecondsOrNull,
                                       String message,
                                       Throwable cause) {
        super(code, STATUS, retryAfterSecondsOrNull, message, cause);
    }
}
