/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

public class B2FileSseForRequest {
    /**
     * The SSE mode, e.g. SSE-B2 or SSE-C
     */
    @B2Json.required
    private final String mode;

    /**
     * The SSE algorithm, e.g. AES256
     */
    @B2Json.required
    private final String algorithm;

    /**
     * The Base64-encoded customer key for SSE-C requests
     */
    @B2Json.optional (omitNull = true)
    private final String customerKey;

    /**
     * The Base64-encoded customer key MD5 for SSE-C requests
     */
    @B2Json.optional (omitNull = true)
    private final String customerKeyMd5;

    @B2Json.constructor(params = "mode, algorithm, customerKey, customerKeyMd5")
    public B2FileSseForRequest(String mode, String algorithm, String customerKeyOrNull, String customerKeyMd5OrNull) {
        B2Preconditions.checkArgumentIsNotNull(mode, "mode");
        B2Preconditions.checkArgumentIsNotNull(algorithm, "algorithm");
        if (mode.equals(B2ServerSideEncryptionMode.SSE_C)) {
            B2Preconditions.checkArgumentIsNotNull(customerKeyOrNull, "customerKeyOrNull");
            B2Preconditions.checkArgumentIsNotNull(customerKeyMd5OrNull, "customerKeyMd5OrNull");
        }

        this.mode = mode;
        this.algorithm = algorithm;
        this.customerKey = customerKeyOrNull;
        this.customerKeyMd5 = customerKeyMd5OrNull;
    }

    public String getMode() { return mode; }

    public String getAlgorithm() { return algorithm; }

    public String getCustomerKey() {
        return customerKey;
    }

    public String getCustomerKeyMd5() {
        return customerKeyMd5;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2FileSseForRequest that = (B2FileSseForRequest) o;
        return Objects.equals(mode, that.getMode()) &&
                Objects.equals(algorithm, that.getAlgorithm()) &&
                Objects.equals(customerKey, that.getCustomerKey()) &&
                Objects.equals(customerKeyMd5, that.getCustomerKeyMd5());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, algorithm, customerKey, customerKeyMd5);
    }

    public String toString() {
        return "B2FileSseForRequest{" +
                "mode='" + mode + "', " +
                "algorithm=" + algorithm + ", " +
                "customerKey=" + customerKey + ", " +
                "customerKeyMd5=" + customerKeyMd5 +
                '}';
    }
}
