/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2FileSseForResponse {
    /**
     * The SSE mode, e.g. SSE-B2 or SSE-C
     */
    @B2Json.optional
    private final String mode;

    /**
     * The SSE algorithm, e.g. AES256
     */
    @B2Json.optional
    private final String algorithm;

    /**
     * The Base64-encoded customer key MD5 for SSE-C requests
     */
    @B2Json.optional (omitNull = true)
    private final String customerKeyMd5;

    @B2Json.constructor(params = "mode, algorithm, customerKeyMd5")
    public B2FileSseForResponse(String mode, String algorithm, String customerKeyMd5) {
        this.mode = mode;
        this.algorithm = algorithm;
        this.customerKeyMd5 = customerKeyMd5;
    }

    public String getMode() { return mode; }

    public String getAlgorithm() { return algorithm; }

    public String getCustomerKeyMd5() { return customerKeyMd5; }

    /**
     * Construct a B2FileSseForResponse from B2Headers, or null if the required headers are not present
     * @param headers B2Headers
     * @return a new B2FileSseForResponse or null
     */
    public static B2FileSseForResponse getEncryptionFromHeadersOrNull(B2Headers headers) {
        if (headers == null) {
            return null;
        }

        // Check for the "X-Bz-Server-Side-Encryption" header
        final String algorithm = headers.getServerSideEncryptionOrNull();
        if (algorithm != null) {
            return new B2FileSseForResponse(B2ServerSideEncryptionMode.SSE_B2, algorithm, null);
        }

        final String customerAlgorithm = headers.getSseCustomerAlgorithmOrNull();
        if (customerAlgorithm != null) {
            return new B2FileSseForResponse(
                    B2ServerSideEncryptionMode.SSE_C,
                    customerAlgorithm,
                    headers.getSseCustomerKeyMd5OrNull()); // may be null for API calls that don't require key/MD5
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2FileSseForResponse that = (B2FileSseForResponse) o;
        return Objects.equals(mode, that.getMode()) &&
                Objects.equals(algorithm, that.getAlgorithm()) &&
                Objects.equals(customerKeyMd5, that.getCustomerKeyMd5());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, algorithm, customerKeyMd5);
    }

    public String toString() {
        return "B2FileSseForResponse{" +
                "mode='" + mode + "', " +
                "algorithm=" + algorithm + ", " +
                "customerKeyMd5=" + customerKeyMd5 +
                "}";
    }
}
