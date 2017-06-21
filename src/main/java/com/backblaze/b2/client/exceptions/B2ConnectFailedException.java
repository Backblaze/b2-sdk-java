/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

/**
 * B2ConnectFailedException represents a failure to connect to the
 * B2 servers.  If this persists, there's probably an issue with
 * your network.
 *
 * Note that this exception is generated locally by the SDK and
 * has a non-standard STATUS code.
 */
public class B2ConnectFailedException extends B2NetworkBaseException {
    static final int STATUS = 901;

    public B2ConnectFailedException(String code,
                                    Integer retryAfterSecondsOrNull,
                                    String message) {
        this(code, retryAfterSecondsOrNull, message, null);
    }

    public B2ConnectFailedException(String code,
                                    Integer retryAfterSecondsOrNull,
                                    String message,
                                    Throwable cause) {
        super(code, STATUS, retryAfterSecondsOrNull, message, cause);
    }
}
