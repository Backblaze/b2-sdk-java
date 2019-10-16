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
 * Response from b2_delete_key, and included in response from b2_list_keys.
 */
public class B2ApplicationKey {

    @B2Json.required
    private final String accountId;

    @B2Json.required
    private final String applicationKeyId;

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

    @B2Json.required
    private final Set<String> options;

    @SuppressWarnings("unused")
    @B2Json.constructor(
            params =
                    "accountId, " +
                    "applicationKeyId, " +
                    "keyName, " +
                    "capabilities, " +
                    "bucketId, " +
                    "namePrefix, " +
                    "expirationTimestamp, " +
                    "options"
    )
    public B2ApplicationKey(String accountId,
                            String applicationKeyId,
                            String keyName,
                            TreeSet<String> capabilities,
                            String bucketId,
                            String namePrefix,
                            Long expirationTimestamp,
                            Set<String> options) {
        
        this.accountId = accountId;
        this.applicationKeyId = applicationKeyId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2ApplicationKey that = (B2ApplicationKey) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(applicationKeyId, that.applicationKeyId) &&
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
                keyName,
                capabilities,
                bucketId,
                namePrefix,
                expirationTimestamp,
                options);
    }
}
