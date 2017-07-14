/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class B2ExceptionTest {
    private static final String CODE = "test";
    private static final Integer RETRY_AFTER_SECS = 123;
    private static final String MSG = "test message";

    @Test
    public void testCreate() {
        checkCreate(B2NotFoundException.STATUS,           B2NotFoundException.class);
        checkCreate(B2BadRequestException.STATUS,         B2BadRequestException.class);
        checkCreate(B2UnauthorizedException.STATUS,       B2UnauthorizedException.class);
        checkCreate(B2ForbiddenException.STATUS,          B2ForbiddenException.class);
        checkCreate(B2RequestTimeoutException.STATUS,     B2RequestTimeoutException.class);
        checkCreate(B2TooManyRequestsException.STATUS,    B2TooManyRequestsException.class);
        checkCreate(B2InternalErrorException.STATUS,      B2InternalErrorException.class);
        checkCreate(B2ServiceUnavailableException.STATUS, B2ServiceUnavailableException.class);
        checkCreate(666,                           B2Exception.class);
    }

    private void checkCreate(int status,
                             Class<?> clazz) {
        final B2Exception e = B2Exception.create(CODE, status, RETRY_AFTER_SECS, MSG);
        assertEquals(clazz, e.getClass());

        assertEquals(CODE, e.getCode());
        assertEquals(status, e.getStatus());
        assertEquals(RETRY_AFTER_SECS, e.getRetryAfterSecondsOrNull());
        assertEquals(MSG, e.getMessage());
    }

    @Test
    public void testNetworkException() {
        final B2Exception e = new B2NetworkException(CODE, RETRY_AFTER_SECS, MSG);
        assertEquals("<B2Exception 904 test: test message>", e.toString());

        assertEquals(CODE, e.getCode());
        assertEquals(B2NetworkException.STATUS, e.getStatus());
        assertEquals(RETRY_AFTER_SECS, e.getRetryAfterSecondsOrNull());
        assertEquals(MSG, e.getMessage());
    }

    @Test
    public void testNotFoundException() {
        // this constructor isn't exercised anywhere else.
        final B2Exception e = new B2NotFoundException(RETRY_AFTER_SECS, MSG);
        assertEquals("<B2Exception 404 not_found: test message>", e.toString());

        assertEquals(B2NotFoundException.CODE, e.getCode());
        assertEquals(B2NotFoundException.STATUS, e.getStatus());
        assertEquals(RETRY_AFTER_SECS, e.getRetryAfterSecondsOrNull());
        assertEquals(MSG, e.getMessage());
    }

}
