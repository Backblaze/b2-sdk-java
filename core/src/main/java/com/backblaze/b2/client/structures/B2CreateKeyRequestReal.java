/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;
import java.util.Set;

/**
 * b2_create_key request, as sent to service, including the accountId.
 *
 * Code that calls B2StorageClient uses B2CreateKeyRequest, which doesn't have the
 * accountId in it.  The B2StorageClient makes this real request by adding the accountId.
 */
public class B2CreateKeyRequestReal {

    @B2Json.required
    private final String accountId;

    @B2Json.required
    private final Set<String> capabilities;

    @B2Json.required
    private final String keyName;

    @B2Json.optional
    private final Long validDurationInSeconds;

    @B2Json.optional
    private final String bucketId;

    @B2Json.optional
    private final String namePrefix;

    @B2Json.constructor(params = "accountId, capabilities, keyName, validDurationInSeconds, bucketId, namePrefix")
    private B2CreateKeyRequestReal(String accountId,
                                   Set<String> capabilities,
                                   String keyName,
                                   Long validDurationInSeconds,
                                   String bucketId,
                                   String namePrefix) {

        this.accountId = accountId;
        this.capabilities = capabilities;
        this.keyName = keyName;
        this.validDurationInSeconds = validDurationInSeconds;
        this.bucketId = bucketId;
        this.namePrefix = namePrefix;
    }

    public B2CreateKeyRequestReal(String accountId, B2CreateKeyRequest mostOfRequest) {
        this(
                accountId,
                mostOfRequest.getCapabilities(),
                mostOfRequest.getKeyName(),
                mostOfRequest.getValidDurationInSeconds(),
                mostOfRequest.getBucketId(),
                mostOfRequest.getNamePrefix()
        );
    }

    @SuppressWarnings("unused")
    public String getAccountId() {
        return accountId;
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
        B2CreateKeyRequestReal that = (B2CreateKeyRequestReal) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(capabilities, that.capabilities) &&
                Objects.equals(keyName, that.keyName) &&
                Objects.equals(validDurationInSeconds, that.validDurationInSeconds) &&
                Objects.equals(bucketId, that.bucketId) &&
                Objects.equals(namePrefix, that.namePrefix);
    }

    @Override
    public int hashCode() {

        return Objects.hash(accountId, capabilities, keyName, validDurationInSeconds, bucketId, namePrefix);
    }
}
