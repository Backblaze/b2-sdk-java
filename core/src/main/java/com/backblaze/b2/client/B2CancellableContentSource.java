/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;

import java.io.IOException;
import java.io.InputStream;

/**
 * A ContentSource wrapper that returns an InputStream that will check whether
 * the cancellation token says to stop.
 */
public class B2CancellableContentSource implements B2ContentSource {
    private final B2ContentSource source;
    private final B2CancellationToken cancellationToken;

    public B2CancellableContentSource(B2ContentSource source, B2CancellationToken cancellationToken) {
        this.source = source;
        this.cancellationToken = cancellationToken;
    }

    @Override
    public long getContentLength() throws IOException {
        return source.getContentLength();
    }

    @Override
    public String getSha1OrNull() throws IOException {
        return source.getSha1OrNull();
    }

    @Override
    public Long getSrcLastModifiedMillisOrNull() throws IOException {
        return source.getSrcLastModifiedMillisOrNull();
    }

    @Override
    public InputStream createInputStream() throws IOException, B2Exception {
        return new CancellableInputStream(source.createInputStream(), cancellationToken);
    }

    private static class CancellableInputStream extends InputStream {
        private final InputStream source;
        private final B2CancellationToken cancellationToken;

        public CancellableInputStream(InputStream source, B2CancellationToken cancellationToken) {
            this.source = source;
            this.cancellationToken = cancellationToken;
        }

        private void throwIfCancelled() throws IOException {
            if (cancellationToken.isCancelled()) {
                throw new IOException("Request was cancelled by caller");
            }
        }

        @Override
        public int read() throws IOException {
            throwIfCancelled();

            return source.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            throwIfCancelled();

            return source.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            throwIfCancelled();

            return source.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            throwIfCancelled();

            return source.skip(n);
        }

        @Override
        public int available() throws IOException {
            throwIfCancelled();

            return source.available();
        }

        @Override
        public void close() throws IOException {
            throwIfCancelled();

            source.close();
        }

        @Override
        public synchronized void mark(int readlimit) {
            source.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            throwIfCancelled();

            source.reset();
        }

        @Override
        public boolean markSupported() {
            return source.markSupported();
        }
    }
}
