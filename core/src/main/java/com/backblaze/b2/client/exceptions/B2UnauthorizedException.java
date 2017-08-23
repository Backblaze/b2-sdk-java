/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.exceptions;

public class B2UnauthorizedException extends B2Exception {
    public static final int STATUS = 401;

    // XXX: ideally this would be "jar private" so far so that only
    // internal classes could use it.  this may be an argument for
    // cramming all the code back into one package.  alternatively,
    // if we allow plugin retry policies, they may need access to
    // this info.
    public enum RequestCategory {
        ACCOUNT_AUTHORIZATION,
        UPLOADING,
        OTHER
    }

    // by default, assume this was for the default category.
    private RequestCategory requestCategory = RequestCategory.OTHER;

    public B2UnauthorizedException(String code,
                                   Integer retryAfterSecondsOrNull,
                                   String message) {
        this(code, retryAfterSecondsOrNull, message, null);
    }

    public B2UnauthorizedException(String code,
                                   Integer retryAfterSecondsOrNull,
                                   String message,
                                   Throwable cause) {
        super(code, STATUS, retryAfterSecondsOrNull, message, cause);
    }

    public RequestCategory getRequestCategory() {
        return requestCategory;
    }
    public void setRequestCategory(RequestCategory requestCategory) {
        this.requestCategory = requestCategory;
    }
}
