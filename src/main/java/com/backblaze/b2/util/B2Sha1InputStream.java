/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Wrapper for an InputStream that computes the SHA1 of the bytes
 * going through it.
 */
public class B2Sha1InputStream extends InputStream {
    // note, we rely on InputStream's behavior of *NOT* implementing mark & reset
    // since computing the sha1 would get more complicated if we had to track
    // which parts of the stream we'd already digested.
    private final InputStream in;
    private final MessageDigest digest;

    /**
     * Initializes a stream that will digest all of the bytes read and skipped.
     */
    public B2Sha1InputStream(InputStream in) {
        this.in = in;
        this.digest = B2Sha1.createSha1MessageDigest();
    }

    /**
     * Returns the SHA1 of the bytes processed so far.
     */
    public byte [] digest() {
        return digest.digest();
    }

    /**
     * Returns the SHA1 if the bytes processed so far, as a hex string.
     */
    public String hexDigest() {
        return B2StringUtil.toHexString(digest());
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public int read() throws IOException {
        int c = in.read();
        if (c != -1) {
            digest.update((byte) c);
        }
        return c;
    }

    @Override
    public int read(byte [] buffer, int offset, int length) throws IOException {
        int byteCount = in.read(buffer, offset, length);
        if (byteCount != -1) {
            digest.update(buffer, offset, byteCount);
        }
        return byteCount;
    }


    @Override
    public long skip(long bytesToSkip) throws IOException {
        long bytesSkipped = 0;
        byte [] buffer = new byte[4096];
        while (bytesSkipped < bytesToSkip) {
            int bytesToRead = (int) Math.min(bytesToSkip - bytesSkipped, buffer.length);
            // This read updates our digest
            int bytesRead = read(buffer, 0, bytesToRead);
            if (bytesRead < 0) {
                break;
            }
            bytesSkipped += bytesRead;
        }
        return bytesSkipped;
    }
}
