/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

/**
 * This structure describes the json that's returned by b2 for errors.
 */
public class B2ErrorStructure {
    /**
     * The HTTP status code being returned.
     *
     * This is redundant with the status code in the header of the response,
     * but it's handy to have it here, to.
     */
    @B2Json.required
    public final int status;

    /**
     * A single token that indicates the cause of the error.  If you
     * need to check for specific error returns, use this, not the message.
     */
    @B2Json.required
    public final String code;

    /**
     * A human-readable string giving details (if available) about what
     * went wrong.
     */
    @B2Json.required
    public final String message;

    @B2Json.constructor(params = "status, code, message")
    public B2ErrorStructure(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
