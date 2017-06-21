/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.backblaze.b2.util.B2StringUtil.toHexString;

public interface B2Sha1 {
    int SHA1_SIZE = 20;
    int HEX_SHA1_SIZE = 2 * SHA1_SIZE;

    /**
     * Returns a SHA-1 MessageDigest, which we expect to always be available.
     */
    static MessageDigest createSha1MessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No SHA-1 installed!", e);
        }
    }

    /**
     * Returns the SHA-1 of the given bytes, as binary data.
     */
    static byte [] binarySha1OfBytes(byte [] data) {
        MessageDigest digest = createSha1MessageDigest();
        digest.update(data);
        return digest.digest();
    }

    /**
     * Returns the SHA-1 of the given bytes, as a hex string.
     */
    static String hexSha1OfBytes(byte [] data) {
        return toHexString(binarySha1OfBytes(data));
    }

    /**
     * Returns the SHA-1 of the given input stream, as a hex string.
     */
    static String hexSha1OfInputStream(InputStream in) throws IOException {
        return toHexString(binarySha1OfInputStream(in));
    }

    /**
     * Returns the SHA-1 of the given InputStream, as binary data.
     */
    static byte [] binarySha1OfInputStream(InputStream in) throws IOException {
        MessageDigest digest = createSha1MessageDigest();
        byte[] bytesBuffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = in.read(bytesBuffer)) != -1) {
            digest.update(bytesBuffer, 0, bytesRead);
        }
        return digest.digest();
    }

    /**
     * This canonicalizes sha1s before comparing them.
     *
     * @param sha1a the first sha1 to compare.  must be non-null.
     * @param sha1b the second sha1 to compare.  must be non-null.
     * @return true iff these represent the same sha1.
     */

    static boolean equalHexSha1s(String sha1a, String sha1b) {
        B2Preconditions.checkArgument(sha1a != null);
        B2Preconditions.checkArgument(sha1b != null);
        final String lowerA = sha1a.toLowerCase();
        final String lowerB = sha1b.toLowerCase();
        return lowerA.equals(lowerB);
    }
}
