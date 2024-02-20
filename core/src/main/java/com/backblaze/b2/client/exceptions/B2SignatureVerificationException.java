/*
 * Copyright 2024, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

/**
 * Instances of this class represents failure to verify content given a signature and secret.
 *
 */
public class B2SignatureVerificationException extends B2Exception {
    public static final String DEFAULT_CODE = "signature_verification_failed";
    public static final int STATUS = 0;

    public B2SignatureVerificationException(String message) {
        super(DEFAULT_CODE, STATUS, null /* no retries */, message);
    }
}
