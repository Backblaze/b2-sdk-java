/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2DeleteKeyRequest {

    @B2Json.required
    final String applicationKeyId;

    @B2Json.constructor(params = "applicationKeyId")
    public B2DeleteKeyRequest(String applicationKeyId) {
        this.applicationKeyId = applicationKeyId;
    }

    @SuppressWarnings("unused")
    public String getApplicationKeyId() {
        return applicationKeyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2DeleteKeyRequest that = (B2DeleteKeyRequest) o;
        return Objects.equals(applicationKeyId, that.applicationKeyId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(applicationKeyId);
    }

    public static Builder builder(String applicationKeyId) {
        return new Builder(applicationKeyId);
    }

    public static class Builder {
        private final String applicationKeyId;

        public Builder(String applicationKeyId) {
            this.applicationKeyId = applicationKeyId;
        }

        public B2DeleteKeyRequest build() {
            return new B2DeleteKeyRequest(applicationKeyId);
        }
    }
}
