/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Information returned from b2_create_key.
 *
 * This is like B2ApplicationKey, with the addition of the secret applicationKey,
 * which is returned when a key is created, but not returned by b2_list_keys,
 * or b2_delete_key.
 */
public class B2CreatedApplicationKey {

    @B2Json.required
    private final String accountId;

    @B2Json.required
    private final String applicationKeyId;

    @B2Json.required
    private final String applicationKey;

    @B2Json.required
    private final String keyName;

    @B2Json.required
    private final TreeSet<String> capabilities;

    @B2Json.optional
    private final String bucketId;

    @B2Json.optional
    private final String namePrefix;

    @B2Json.optional
    private final Long expirationTimestamp;

    @B2Json.optional
    private final Set<String> options;

    @SuppressWarnings("unused")
    @B2Json.constructor(
            params =
                    "accountId, " +
                    "applicationKeyId, " +
                    "applicationKey, " +
                    "keyName, " +
                    "capabilities, " +
                    "bucketId, " +
                    "namePrefix, " +
                    "expirationTimestamp, " +
                    "options"
    )
    public B2CreatedApplicationKey(String accountId,
                                   String applicationKeyId,
                                   String applicationKey,
                                   String keyName,
                                   TreeSet<String> capabilities,
                                   String bucketId,
                                   String namePrefix,
                                   Long expirationTimestamp,
                                   Set<String> options) {

        this.accountId = accountId;
        this.applicationKeyId = applicationKeyId;
        this.applicationKey = applicationKey;
        this.keyName = keyName;
        this.capabilities = capabilities;
        this.bucketId = bucketId;
        this.namePrefix = namePrefix;
        this.expirationTimestamp = expirationTimestamp;
        this.options = options;
    }

    public String getAccountId() {
        return accountId;
    }

    @SuppressWarnings("unused")
    public String getApplicationKeyId() {
        return applicationKeyId;
    }

    @SuppressWarnings("unused")
    public String getApplicationKey() {
        return applicationKey;
    }

    @SuppressWarnings("unused")
    public String getKeyName() {
        return keyName;
    }

    @SuppressWarnings("unused")
    public TreeSet<String> getCapabilities() {
        return capabilities;
    }

    @SuppressWarnings("unused")
    public String getBucketId() {
        return bucketId;
    }

    @SuppressWarnings("unused")
    public String getNamePrefix() {
        return namePrefix;
    }

    @SuppressWarnings("unused")
    public Long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    @SuppressWarnings("unused")
    public Set<String> getOptions() { return options; }

    /**
     * Converts to the B2ApplicationKey structure, as returned from b2_list_keys,
     * which does not contain the secret key.
     */
    public B2ApplicationKey toApplicationKey() {
        return new B2ApplicationKey(
                accountId,
                applicationKeyId,
                keyName,
                capabilities,
                bucketId,
                namePrefix,
                expirationTimestamp,
                options
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2CreatedApplicationKey that = (B2CreatedApplicationKey) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(applicationKeyId, that.applicationKeyId) &&
                Objects.equals(applicationKey, that.applicationKey) &&
                Objects.equals(keyName, that.keyName) &&
                Objects.equals(capabilities, that.capabilities) &&
                Objects.equals(bucketId, that.bucketId) &&
                Objects.equals(namePrefix, that.namePrefix) &&
                Objects.equals(expirationTimestamp, that.expirationTimestamp) &&
                Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                accountId,
                applicationKeyId,
                applicationKey,
                keyName,
                capabilities,
                bucketId,
                namePrefix,
                expirationTimestamp,
                options);
    }
}
