/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

/**
 * Use this to create a stream that contains the contents a wrapped stream
 * concatenated with the sha1 of that stream.
 */
public class B2Sha1AppenderInputStream {

    private static class LazySha1Stream extends InputStream {
        // note, we rely on InputStream's behavior of *NOT* implementing mark & reset
        // since B2Sha1InputStream doesn't support mark & reset.  uses default implementation
        // of skip() -- since it turns out the SequenceInputStream doesn't call it anyway.

        private final B2Sha1InputStream original;
        private InputStream sha1Stream;

        private LazySha1Stream(B2Sha1InputStream original) {
            this.original = original;
        }

        @Override
        public int read() throws IOException {
            return getSha1Stream().read();
        }

        @Override
        public int read(byte[] b,
                        int off,
                        int len) throws IOException {
            return getSha1Stream().read(b, off, len);
        }

        @Override
        public int available() throws IOException {
            return getSha1Stream().available();
        }

        @Override
        public void close() throws IOException {
            getSha1Stream().close();
        }

        private InputStream getSha1Stream() {
            if (sha1Stream == null) {
                final byte[] sha1 = B2StringUtil.getUtf8Bytes(original.hexDigest());
                sha1Stream = new ByteArrayInputStream(sha1);
            }
            return sha1Stream;
        }
    }

    public static InputStream create(InputStream original) {
        final B2Sha1InputStream wrappedOriginal = new B2Sha1InputStream(original);
        return new SequenceInputStream(wrappedOriginal, new LazySha1Stream(wrappedOriginal));
    }
}
