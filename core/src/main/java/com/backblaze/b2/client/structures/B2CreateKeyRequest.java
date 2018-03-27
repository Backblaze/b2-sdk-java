/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;
import java.util.Set;

public class B2CreateKeyRequest {

    @B2Json.required
    private final String accountId;

    @B2Json.required
    private final Set<B2Capability> capabilies;

    @B2Json.required
    private final String keyName;

    @B2Json.optional
    private final Long validDurationSeconds;

    @B2Json.optional
    private final String bucketId;

    @B2Json.optional
    private final String namePrefix;

    @B2Json.constructor(params = "accountId, capabilities, keyName, validDurationSeconds, bucketId, namePrefix")
    public B2CreateKeyRequest(String accountId, Set<B2Capability> capabilies, String keyName, Long validDurationSeconds, String bucketId, String namePrefix) {
        this.accountId = accountId;
        this.capabilies = capabilies;
        this.keyName = keyName;
        this.validDurationSeconds = validDurationSeconds;
        this.bucketId = bucketId;
        this.namePrefix = namePrefix;
    }

    public static Builder builder(String accountId, Set<B2Capability> capabilities, String keyName) {
        return new Builder(accountId, capabilities, keyName);
    }

    @SuppressWarnings("unused")
    public String getAccountId() {
        return accountId;
    }

    @SuppressWarnings("unused")
    public Set<B2Capability> getCapabilies() {
        return capabilies;
    }

    @SuppressWarnings("unused")
    public String getKeyName() {
        return keyName;
    }

    @SuppressWarnings("unused")
    public Long getValidDurationSeconds() {
        return validDurationSeconds;
    }

    @SuppressWarnings("unused")
    public String getBucketId() {
        return bucketId;
    }

    @SuppressWarnings("unused")
    public String getNamePrefix() {
        return namePrefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2CreateKeyRequest that = (B2CreateKeyRequest) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(capabilies, that.capabilies) &&
                Objects.equals(keyName, that.keyName) &&
                Objects.equals(validDurationSeconds, that.validDurationSeconds) &&
                Objects.equals(bucketId, that.bucketId) &&
                Objects.equals(namePrefix, that.namePrefix);
    }

    @Override
    public int hashCode() {

        return Objects.hash(accountId, capabilies, keyName, validDurationSeconds, bucketId, namePrefix);
    }

    private static class Builder {

        private final String accountId;
        private final Set<B2Capability> capabilities;
        private final String keyName;
        private Long validDurationSeconds = null;
        private String bucketId = null;
        private String namePrefix = null;

        private Builder(String accountId, Set<B2Capability> capabilities, String keyName) {
            this.accountId = accountId;
            this.capabilities = capabilities;
            this.keyName = keyName;
        }

        @SuppressWarnings("unused")
        public Builder setValidDurationSeconds(Long validDurationSeconds) {
            this.validDurationSeconds = validDurationSeconds;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder setBucketId(String bucketId) {
            this.bucketId = bucketId;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder setNamePrefix(String namePrefix) {
            this.namePrefix = namePrefix;
            return this;
        }

        public B2CreateKeyRequest build() {
            return new B2CreateKeyRequest(accountId, capabilities, keyName, validDurationSeconds, bucketId, namePrefix);
        }
    }
}
