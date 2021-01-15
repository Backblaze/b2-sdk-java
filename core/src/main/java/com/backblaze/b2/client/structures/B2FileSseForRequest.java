/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Md5;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Base64;
import java.util.Objects;

import static com.backblaze.b2.client.structures.B2ServerSideEncryptionMode.SSE_B2;
import static com.backblaze.b2.client.structures.B2ServerSideEncryptionMode.SSE_C;

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

    private static final B2FileSseForRequest SSE_B2_AES256 =
            new B2FileSseForRequest(SSE_B2, "AES256", null, null);

    @B2Json.constructor(params = "mode, algorithm, customerKey, customerKeyMd5")
    private B2FileSseForRequest(String mode, String algorithm, String customerKeyOrNull, String customerKeyMd5OrNull) {
        B2Preconditions.checkArgumentIsNotNull(mode, "mode");
        B2Preconditions.checkArgumentIsNotNull(algorithm, "algorithm");
        if (mode.equals(SSE_C)) {
            B2Preconditions.checkArgumentIsNotNull(customerKeyOrNull, "customerKeyOrNull");
            B2Preconditions.checkArgumentIsNotNull(customerKeyMd5OrNull, "customerKeyMd5OrNull");
        }

        this.mode = mode;
        this.algorithm = algorithm;
        this.customerKey = customerKeyOrNull;
        this.customerKeyMd5 = customerKeyMd5OrNull;
    }

    /**
     * Creates and returns a B2FileSseForRequest for SSE-B2 with algorithm set to AES256.
     * @return B2FileSseForRequest with mode=SSE-B2 and algorithm=AES256
     */
    public static B2FileSseForRequest createSseB2Aes256() {
        return SSE_B2_AES256;
    }

    /**
     * Creates and returns a B2FileSseForRequest for SSE-C with algorithm set to AES256 and encryption key as specified.
     * @param customerKey customer encryption key encoded in Base64
     * @return B2FileSseForRequest with mode=SSE-C and algorithm=AES256 and customer key & MD5 set according to
     * input parameter.
     */
    public static B2FileSseForRequest createSseCAes256(String customerKey) {
        final byte[] customerKeyBytes = Base64.getDecoder().decode(customerKey);
        final String customerKeyMd5 = Base64.getEncoder().encodeToString(B2Md5.binaryMd5OfBytes(customerKeyBytes));
        return new B2FileSseForRequest(SSE_C, "AES256", customerKey, customerKeyMd5);
    }

    /**
     * Creates and returns a B2FileSseForRequest for SSE-C with algorithm set to AES256 and key/MD5 as specified.
     * @param customerKey customer encryption key encoded in Base64
     * @param customerKeyMd5 customer encryption key MD5 digest encoded in Base64
     * @return B2FileSseForRequest with mode=SSE-C and algorithm=AES256 and customer key & MD5 set according to input parameters.
     */
    public static B2FileSseForRequest createSseCAes256(String customerKey, String customerKeyMd5) {
        return new B2FileSseForRequest(SSE_C, "AES256", customerKey, customerKeyMd5);
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
