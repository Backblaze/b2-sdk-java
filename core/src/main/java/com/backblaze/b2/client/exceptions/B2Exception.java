/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

/**
 * B2Exception is a checked exception class that's the base class for
 * all B2-specific exceptions thrown by b2sdk4j.
 */
public class B2Exception extends Exception {
    private final String code;
    private final int status;

    // if retryAfterSecondsOrNull is non-null, we should wait that long before
    // attempting request.
    private final Integer retryAfterSecondsOrNull;


    public B2Exception(String code,
                       int status,
                       Integer retryAfterSecondsOrNull,
                       String message) {
        this(code, status, retryAfterSecondsOrNull, message, null);
    }

    public B2Exception(String code,
                       int status,
                       Integer retryAfterSecondsOrNull,
                       String message,
                       Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
        this.retryAfterSecondsOrNull = retryAfterSecondsOrNull;
    }

    public String toString() {
        return "<B2Exception " + status + " " + code + ": " + getMessage() + ">";
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public Integer getRetryAfterSecondsOrNull() {
        return retryAfterSecondsOrNull;
    }

    /**
     * Returns a new B2Exception, trying to provide the appropriate subclass to
     * characterize the it.
     *
     * @param code the code from the error response
     * @param status the http status from the response.
     * @param retryAfterSecondsOrNull the value of 'Retry-After' header if it's an integer number of seconds.
     * @param message the message in the error response.
     * @return a new B2Exception (maybe an instance of one of its subclasses)
     */
    public static B2Exception create(String code,
                                     int status,
                                     Integer retryAfterSecondsOrNull,
                                     String message) {
        switch (status) {
            case B2NotFoundException.STATUS:
                return new B2NotFoundException(code, retryAfterSecondsOrNull, message);
            case B2BadRequestException.STATUS:
                return new B2BadRequestException(code, retryAfterSecondsOrNull, message);
            case B2UnauthorizedException.STATUS:
                return new B2UnauthorizedException(code, retryAfterSecondsOrNull, message);
            case B2ForbiddenException.STATUS:
                return new B2ForbiddenException(code, retryAfterSecondsOrNull, message);
            case B2RequestTimeoutException.STATUS:
                return new B2RequestTimeoutException(code, retryAfterSecondsOrNull, message);
            case B2TooManyRequestsException.STATUS:
                return new B2TooManyRequestsException(code, retryAfterSecondsOrNull, message);
            case B2InternalErrorException.STATUS:
                return new B2InternalErrorException(code, retryAfterSecondsOrNull, message);
            case B2ServiceUnavailableException.STATUS:
                return new B2ServiceUnavailableException(code, retryAfterSecondsOrNull, message);

            default:
                return new B2Exception(code, status, retryAfterSecondsOrNull, message);
        }
    }
}
