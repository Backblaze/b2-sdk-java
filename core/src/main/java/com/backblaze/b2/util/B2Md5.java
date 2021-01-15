/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.backblaze.b2.util.B2StringUtil.toHexString;

public interface B2Md5 {
    int MD5_SIZE = 16;
    int HEX_MD5_SIZE = 2 * MD5_SIZE;

    /**
     * Returns an MD5 MessageDigest, which we expect to always be available.
     */
    static MessageDigest getMd5MessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No MD5 installed!", e);
        }
    }

    /**
     * Returns the binary digest of an array of bytes
     */
    static byte[] binaryMd5OfBytes(byte[] bytes) {
        return binaryMd5OfBytes(bytes, 0, bytes.length);
    }

    /**
     * Returns the binary digest of a subsequence of an array of bytes
     */
    static byte[] binaryMd5OfBytes(byte[] bytes, int offset, int length) {
        final MessageDigest digest = getMd5MessageDigest();
        digest.update(bytes, offset, length);
        return digest.digest();
    }

    /**
     * Returns the hex digest of an array of bytes.
     */
    static String hexMd5OfBytes(byte[] bytes) {
        return hexMd5OfBytes(bytes, 0, bytes.length);
    }

    /**
     * Returns the hex digest of an array of bytes.
     */
    static String hexMd5OfBytes(byte[] bytes, int offset, int length) {
        return toHexString(binaryMd5OfBytes(bytes, offset, length));
    }
}
