/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.TreeSet;

/**
 * Response from b2_create_key and b2_delete_key.  Included in response from b2_list_keys.
 */
public class B2ApplicationKey {

    @B2Json.required
    private final String accountId;

    @B2Json.required
    private final String applicationKeyId;

    /**
     * This applicationKey is always returned from b2_create_key, and never from
     * b2_list_keys nor b2_delete_key.  It's optional here so this structure for
     * all three calls.
     */
    @B2Json.optional
    private final String applicationKey;

    @B2Json.required
    private final String keyName;

    @B2Json.required
    private final TreeSet<B2Capability> capabilities;

    @B2Json.optional
    private final String bucketId;

    @B2Json.optional
    private final String namePrefix;

    @B2Json.optional
    private final Long expirationTimestamp;

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
                    "expirationTimestamp"
    )
    public B2ApplicationKey(String accountId,
                            String applicationKeyId,
                            String applicationKey,
                            String keyName,
                            TreeSet<B2Capability> capabilities,
                            String bucketId,
                            String namePrefix,
                            Long expirationTimestamp) {
        
        this.accountId = accountId;
        this.applicationKeyId = applicationKeyId;
        this.applicationKey = applicationKey;
        this.keyName = keyName;
        this.capabilities = capabilities;
        this.bucketId = bucketId;
        this.namePrefix = namePrefix;
        this.expirationTimestamp = expirationTimestamp;
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
    public TreeSet<B2Capability> getCapabilities() {
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
}
