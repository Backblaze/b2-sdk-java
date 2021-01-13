/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2BucketServerSideEncryption {
    /**
     * The SSE mode, e.g., SSE-B2 (or "unauthorized")
     */
    @B2Json.required
    private final String mode;

    /**
     * The SSE algorithm, i.e., AES256 (omitted if mode is "unauthorized").
     */
    @B2Json.optional
    private final String algorithm;

    @B2Json.constructor(params = "mode, algorithm")
    public B2BucketServerSideEncryption(String mode, String algorithm) {
        this.mode = mode;
        this.algorithm = algorithm;
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
