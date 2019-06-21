/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

public class B2CannotComputeException extends B2LocalException {

    public B2CannotComputeException(String message) {
        super("cannot compute", message);
    }
}
