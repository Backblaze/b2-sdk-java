/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import java.util.Objects;
import java.util.Set;

public class B2CreateKeyRequest {

    private final Set<String> capabilities;
    private final String keyName;
    private final Long validDurationInSeconds;
    private final String bucketId;
    private final String namePrefix;

    private B2CreateKeyRequest(Set<String> capabilities,
                               String keyName,
                               Long validDurationInSeconds,
                               String bucketId,
                               String namePrefix) {

        this.capabilities = capabilities;
        this.keyName = keyName;
        this.validDurationInSeconds = validDurationInSeconds;
        this.bucketId = bucketId;
        this.namePrefix = namePrefix;
    }

    public static Builder builder(Set<String> capabilities, String keyName) {
        return new Builder(capabilities, keyName);
    }

    @SuppressWarnings("unused")
    public Set<String> getCapabilities() {
        return capabilities;
    }

    @SuppressWarnings("unused")
    public String getKeyName() {
        return keyName;
    }

    @SuppressWarnings("unused")
    public Long getValidDurationInSeconds() {
        return validDurationInSeconds;
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
        return Objects.equals(capabilities, that.capabilities) &&
                Objects.equals(keyName, that.keyName) &&
                Objects.equals(validDurationInSeconds, that.validDurationInSeconds) &&
                Objects.equals(bucketId, that.bucketId) &&
                Objects.equals(namePrefix, that.namePrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capabilities, keyName, validDurationInSeconds, bucketId, namePrefix);
    }

    public static class Builder {

        private final Set<String> capabilities;
        private final String keyName;
        private Long validDurationInSeconds = null;
        private String bucketId = null;
        private String namePrefix = null;

        private Builder(Set<String> capabilities, String keyName) {
            this.capabilities = capabilities;
            this.keyName = keyName;
        }

        @SuppressWarnings("unused")
        public Builder setValidDurationInSeconds(Long validDurationInSeconds) {
            this.validDurationInSeconds = validDurationInSeconds;
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
            return new B2CreateKeyRequest(capabilities, keyName, validDurationInSeconds, bucketId, namePrefix);
        }
    }
}
