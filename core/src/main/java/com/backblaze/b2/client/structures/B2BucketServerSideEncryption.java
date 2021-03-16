/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.json.B2Json;

import java.util.Objects;

import static com.backblaze.b2.client.structures.B2ServerSideEncryptionMode.NONE;
import static com.backblaze.b2.client.structures.B2ServerSideEncryptionMode.SSE_B2;

public class B2BucketServerSideEncryption {
    /**
     * The SSE mode, e.g., SSE-B2
     */
    @B2Json.optional
    private final String mode;

    /**
     * The SSE algorithm, i.e., AES256
     */
    @B2Json.optional
    private final String algorithm;

    private static final B2BucketServerSideEncryption SSE_B2_AES256 =
            new B2BucketServerSideEncryption(SSE_B2, "AES256");

    private static final B2BucketServerSideEncryption SSE_NONE =
            new B2BucketServerSideEncryption(NONE, null);

    @B2Json.constructor(params = "mode, algorithm")
    private B2BucketServerSideEncryption(String mode, String algorithm) {
        this.mode = mode;
        this.algorithm = algorithm;
    }

    /**
     * Create default bucket server-side encryption configuration with mode of SSE-B2 and algorithm of AES256
     * @return default SSE-B2 AES256 bucket encryption configuration
     */
    public static B2BucketServerSideEncryption createSseB2Aes256() {
        return SSE_B2_AES256;
    }

    /**
     * Create default bucket server-side encryption configuration with default encryption disabled.
     * @return default disabled bucket encryption configuration
     */
    public static B2BucketServerSideEncryption createSseNone() {
        return SSE_NONE;
    }

    public String getMode() { return mode; }

    public String getAlgorithm() { return algorithm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2BucketServerSideEncryption that = (B2BucketServerSideEncryption) o;
        return Objects.equals(mode, that.getMode()) &&
                Objects.equals(algorithm, that.getAlgorithm());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, algorithm);
    }

    public String toString() {
        return "B2BucketServerSideEncryption{" +
                "mode='" + mode + "', " +
                "algorithm=" + algorithm +
                '}';
    }
}
