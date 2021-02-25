/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

/**
 * Legal hold response representation for B2 API
 */
public class B2LegalHold {

    /**
     * legal hold status
     */
    @B2Json.required
    private final String status;

    @B2Json.constructor(params = "status")
    public B2LegalHold(String status) {
        B2Preconditions.checkArgument(status != null, "status cannot be null");
        this.status = status;
    }

    public static B2LegalHold getLegalHoldFromHeadersOrNull(B2Headers headers) {
        if (headers == null ) {
            return null;
        }

        final String legalHoldStatusOrNull = headers.getLegalHoldOrNull();
        return legalHoldStatusOrNull == null ? null : new B2LegalHold(legalHoldStatusOrNull);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final B2LegalHold that = (B2LegalHold) o;
        return Objects.equals(status, that.status);
    }

    public String getStatus() {
        return this.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status);
    }

    @Override
    public String toString() {
        return "B2LegalHold{" +
                "status=" + status +
                '}';
    }
}
