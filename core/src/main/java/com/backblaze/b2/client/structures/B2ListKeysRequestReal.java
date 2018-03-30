/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;


import com.backblaze.b2.json.B2Json;

import java.util.Objects;

/**
 * NOTE:
 * B2ListKeysRequestReal has the attributes needed by the B2 API.  That's why it's name ends with 'Real'.
 * Code that calls B2StorageClient uses B2ListKeysRequest (with no 'Real' at the end) instead.
 * The B2StorageClient creates a 'Real' request by adding the accountId to the non-real version before
 * sending it to the webifier.
 */
public class B2ListKeysRequestReal {

    @B2Json.required
    private final String accountId;

    @B2Json.optional
    private final Integer maxKeyCount;

    @B2Json.optional
    private final String startApplicationKeyId;

    @B2Json.constructor(params = "accountId, maxKeyCount, startApplicationKeyId")
    public B2ListKeysRequestReal(String accountId, Integer maxKeyCount, String startApplicationKeyId) {
        this.accountId = accountId;
        this.maxKeyCount = maxKeyCount;
        this.startApplicationKeyId = startApplicationKeyId;
    }

    @SuppressWarnings("unused")
    public String getAccountId() {
        return accountId;
    }

    @SuppressWarnings("unused")
    public Integer getMaxKeyCount() {
        return maxKeyCount;
    }

    @SuppressWarnings("unused")
    public String getStartApplicationKeyId() {
        return startApplicationKeyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2ListKeysRequestReal that = (B2ListKeysRequestReal) o;
        return Objects.equals(accountId, that.accountId) &&
                Objects.equals(maxKeyCount, that.maxKeyCount) &&
                Objects.equals(startApplicationKeyId, that.startApplicationKeyId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(accountId, maxKeyCount, startApplicationKeyId);
    }
}

