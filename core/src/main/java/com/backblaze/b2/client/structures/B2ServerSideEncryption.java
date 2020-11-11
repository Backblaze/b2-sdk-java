/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2ServerSideEncryption {
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
    public B2ServerSideEncryption(String mode, String algorithm) {
        this.mode = mode;
        this.algorithm = algorithm;
    }

    public String getMode() { return mode; }

    public String getAlgorithm() { return algorithm; }


    /**
     * Construct a B2ServerSideEncryption from B2Headers, or null if the required headers are not present
     * @param headers B2Headers
     * @return a new B2ServerSideEncryption or null
     */
    public static B2ServerSideEncryption getEncryptionFromHeadersOrNull(B2Headers headers) {
        if (headers == null) {
            return null;
        }

        // Check for the "X-Bz-Server-Side-Encryption" header
        final String algorithm = headers.getServerSideEncryptionOrNull();
        if (algorithm != null) {
            return new B2ServerSideEncryption(B2ServerSideEncryptionMode.SSE_B2, algorithm);
        }

        final String customerAlgorithm = headers.getCustomerServerSideEncryptionOrNull();
        if (customerAlgorithm != null) {
            return new B2ServerSideEncryption(B2ServerSideEncryptionMode.SSE_C, customerAlgorithm);
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
        B2ServerSideEncryption that = (B2ServerSideEncryption) o;
        return Objects.equals(mode, that.getMode()) &&
                Objects.equals(algorithm, that.getAlgorithm());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, algorithm);
    }

    public String toString() {
        return "B2ServerSideEncryption{" +
                "mode='" + mode + "', " +
                "algorithm=" + algorithm +
                '}';
    }
}
