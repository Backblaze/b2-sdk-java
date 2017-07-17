/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

/**
 * Instances of B2LocalException represent issues that are detected by the SDK.
 *
 * Note that these exceptions are generated locally by the SDK and
 * have non-standard STATUS code.
 */
public class B2LocalException extends B2Exception {
    static final int STATUS = 999;

    public B2LocalException(String code,
                            String message) {
        this(code, message, null);
    }

    public B2LocalException(String code,
                            String message,
                            Throwable cause) {
        super(code, STATUS, null /*retryAfterSecs*/, message, cause);
    }
}
