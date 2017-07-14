/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

/**
 * Instances of subclasses of B2NetworkExceptions represent issues that appears
 * to be related to the network.
 *
 * Note that these exceptions are generated locally by the SDK and
 * have non-standard STATUS code.
 */
public abstract class B2NetworkBaseException extends B2Exception {
    B2NetworkBaseException(String code,
                           int status,
                           Integer retryAfterSecondsOrNull,
                           String message,
                           Throwable cause) {
        super(code, status, retryAfterSecondsOrNull, message, cause);
    }
}
