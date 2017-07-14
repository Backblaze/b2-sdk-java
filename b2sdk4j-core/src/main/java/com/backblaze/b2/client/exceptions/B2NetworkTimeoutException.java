/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

/**
 * B2NetworkTimeoutException represents the network connection to
 * a B2 server being closed locally because the server
 * isn't answering in a timely fashion.
 *
 * Note that this exception is generated locally by the SDK and
 * has a non-standard STATUS code.
 */
public class B2NetworkTimeoutException extends B2NetworkBaseException {
    static final int STATUS = 903;

    public B2NetworkTimeoutException(String code,
                                     Integer retryAfterSecondsOrNull,
                                     String message) {
        this(code, retryAfterSecondsOrNull, message, null);
    }

    public B2NetworkTimeoutException(String code,
                                     Integer retryAfterSecondsOrNull,
                                     String message,
                                     Throwable cause) {
        super(code, STATUS, retryAfterSecondsOrNull, message, cause);
    }
}
