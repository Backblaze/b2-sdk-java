/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.exceptions.B2ForbiddenException;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

/**
 * A generic class to represent response fields that are filtered based on authorization
 *
 * Parameter T will represent the different types of response field (e.g. B2FileRetention, B2BucketServerSideEncryption, etc.)
 *
 * This class isn't really for general use, but we need it public in order to build B2FileVersion outside this package.
 */
public class B2AuthorizationFilteredResponseField<T> {

    @B2Json.required
    private final boolean isClientAuthorizedToRead;

    @B2Json.optional
    private final T value;

    @B2Json.constructor(params = "isClientAuthorizedToRead, value")
    public B2AuthorizationFilteredResponseField(boolean isClientAuthorizedToRead, T value) {
        B2Preconditions.checkArgument((isClientAuthorizedToRead || value == null), "value must be null if isClientAuthorizedToRead is false");

        this.isClientAuthorizedToRead = isClientAuthorizedToRead;
        this.value = value;
    }

    T getValue() throws B2ForbiddenException {
        if (!isClientAuthorizedToRead()) {
            throw new B2ForbiddenException(null, null, "client is not authorized to read value");
        }

        return this.value;
    }

    boolean isClientAuthorizedToRead() {
        return this.isClientAuthorizedToRead;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final B2AuthorizationFilteredResponseField<T> that = (B2AuthorizationFilteredResponseField<T>) o;
        return isClientAuthorizedToRead == that.isClientAuthorizedToRead && Objects.equals(value,  that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isClientAuthorizedToRead, value);
    }

    @Override
    public String toString() {
        return "B2AuthorizationFilteredResponseField(" +
                "isClientAuthorizedToRead=" + isClientAuthorizedToRead + "," +
                "value={" +  value + "}" +
                ")";
    }
}
