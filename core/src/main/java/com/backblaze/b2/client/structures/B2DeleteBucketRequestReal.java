/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2DeleteBucketRequestReal {
    @B2Json.required
    private final String accountId;
    @B2Json.required
    private final String bucketId;

    @B2Json.constructor(params = "accountId,bucketId")
    public B2DeleteBucketRequestReal(String accountId,
                                     String bucketId) {
        this.accountId = accountId;
        this.bucketId = bucketId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getBucketId() {
        return bucketId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2DeleteBucketRequestReal that = (B2DeleteBucketRequestReal) o;
        return Objects.equals(getAccountId(), that.getAccountId()) &&
                Objects.equals(getBucketId(), that.getBucketId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAccountId(), getBucketId());
    }
}
