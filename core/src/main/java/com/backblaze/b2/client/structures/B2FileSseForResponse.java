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
    @B2Json.required
    private final String mode;

    /**
     * The SSE algorithm, e.g. AES256
     */
    @B2Json.required
    private final String algorithm;

    @B2Json.constructor(params = "mode, algorithm")
    public B2FileSseForResponse(String mode, String algorithm) {
        this.mode = mode;
        this.algorithm = algorithm;
    }

    public String getMode() { return mode; }

    public String getAlgorithm() { return algorithm; }


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
            return new B2FileSseForResponse(B2ServerSideEncryptionMode.SSE_B2, algorithm);
        }

        final String customerAlgorithm = headers.getCustomerServerSideEncryptionOrNull();
        if (customerAlgorithm != null) {
            return new B2FileSseForResponse(B2ServerSideEncryptionMode.SSE_C, customerAlgorithm);
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
                Objects.equals(algorithm, that.getAlgorithm());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, algorithm);
    }

    public String toString() {
        return "B2FileSseForResponse{" +
                "mode='" + mode + "', " +
                "algorithm=" + algorithm +
                '}';
    }
}
