/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Objects;

public class B2ListKeysResponse {

    @B2Json.required
    private final List<B2ApplicationKey> keys;

    @B2Json.optional
    private final String nextApplicationKeyId;

    @B2Json.constructor(params = "keys, nextApplicationKeyId")
    public B2ListKeysResponse(List<B2ApplicationKey> keys, String nextApplicationKeyId) {
        this.keys = keys;
        this.nextApplicationKeyId = nextApplicationKeyId;
    }

    @SuppressWarnings("unused")
    public List<B2ApplicationKey> getKeys() {
        return keys;
    }

    @SuppressWarnings("unused")
    public String getNextApplicationKeyId() {
        return nextApplicationKeyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2ListKeysResponse that = (B2ListKeysResponse) o;
        return Objects.equals(keys, that.keys) &&
                Objects.equals(nextApplicationKeyId, that.nextApplicationKeyId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(keys, nextApplicationKeyId);
    }
}
