/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

/**
 * B2RuntimeExceptions are thrown instead of B2Exceptions in the places
 * where an interface prevents the SDK from throwing a B2Exception.
 * The most notable examples of this are in the creation, hasNext(), and
 * next() methods of iterators provided by the SDK.
 */
public class B2RuntimeException extends RuntimeException {
    public B2RuntimeException(String message) {
        super(message);
    }

    public B2RuntimeException(String message,
                              Throwable cause) {
        super(message, cause);
    }
}
