/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * B2InputStreamWithByteProgressListener provides progress notifications
 * to a listener.
 *
 * If it throws an exception at any point, its state becomes
 * undefined and you should stop using it.
 *
 * THREAD-SAFETY: this class is NOT thread-safe on its own.
 */
public class B2InputStreamWithByteProgressListener extends InputStream {
    private static final int EOF = -1;
    private final InputStream inputStream;
    private final B2ByteProgressListener listener;

    // how many bytes have we read so far?
    private long nBytesSoFar;


    public B2InputStreamWithByteProgressListener(InputStream inputStream,
                                                 B2ByteProgressListener listener) {
        this.inputStream = inputStream;
        this.listener = listener;
    }

    @Override
    public int read() throws IOException {
        try {
            int value = inputStream.read();
            if (value == EOF) {
                notifyListenerOfRead(EOF);
            } else {
                notifyListenerOfRead(1);
            }
            return value;
        } catch (IOException | RuntimeException e) {
            listener.hitException(e, nBytesSoFar);
            throw e;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b,
                    int off,
                    int len) throws IOException {
        try {
            int nReadOrEof = inputStream.read(b, off,len);
            notifyListenerOfRead(nReadOrEof);
            return nReadOrEof;
        } catch (IOException | RuntimeException e) {
            listener.hitException(e, nBytesSoFar);
            throw e;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        try {
            long nReadOrEof = inputStream.skip(n);
            notifyListenerOfRead(nReadOrEof);
            return nReadOrEof;
        } catch (IOException | RuntimeException e) {
            listener.hitException(e, nBytesSoFar);
            throw e;
        }
    }

    @Override
    public int available() throws IOException {
        try {
            return inputStream.available();
        } catch (IOException | RuntimeException e) {
            listener.hitException(e, nBytesSoFar);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }


    private void notifyListenerOfRead(long nBytesOrEof) {
        if (nBytesOrEof == EOF) {
            listener.reachedEof(nBytesSoFar);
        } else {
            nBytesSoFar += nBytesOrEof;
            listener.progress(nBytesSoFar);
        }
    }

}
