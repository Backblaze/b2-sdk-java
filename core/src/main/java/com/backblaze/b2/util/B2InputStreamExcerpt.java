/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * B2InputStreamExcerpt is a stream that provides the contents
 * of an underlying stream starting from the start'th byte
 * and continuing up to and including the (start + length - 1)'th
 * byte.
 *
 * It relies on skip() working and working efficiently.
 * It does NOT support mark/reset.
 *
 * If it throws an exception at any point, its state becomes
 * undefined and you should stop using it.
 *
 * THREAD-SAFETY: this class is NOT thread-safe on its own.
 */
public class B2InputStreamExcerpt extends InputStream {
    private static final int EOF = -1;
    private final InputStream inputStream;

    // these index values are zero-based and in inputStream's byte stream.
    private final long start;
    private long pastEnd; // we should never return the byte at this position.  it's past the pastEnd.
    private boolean haveDoneInitialSkip;
    private long iPosition;

    public B2InputStreamExcerpt(InputStream inputStream,
                                long start,
                                long length) {
        B2Preconditions.checkArgument(start >= 0, "start must be non-negative.");
        this.inputStream = inputStream;
        this.start = start;
        this.pastEnd = start + length;
        this.haveDoneInitialSkip = false;
        this.iPosition = 0;
    }

    @Override
    public int read() throws IOException {
        startIfNeeded();
        if (atEnd()) {
            return EOF;
        } else {
            iPosition++;
            return inputStream.read();
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
        startIfNeeded();
        if (atEnd()) {
            return EOF;
        }
        int actuallyRead = inputStream.read(b, off, Math.min(getRemaining(), len));
        if (actuallyRead == -1) {
            // let's remember we're at the end.
            pastEnd = iPosition;
        } else if (actuallyRead > 0) {
            iPosition += actuallyRead;
        }
        return actuallyRead;
    }

    @Override
    public long skip(long n) throws IOException {
        startIfNeeded();
        if (atEnd()) {
            return 0;
        }
        final long actuallySkipped = super.skip(Math.min(n,getRemaining()));
        iPosition += actuallySkipped;
        return actuallySkipped;
    }

    @Override
    public int available() throws IOException {
        startIfNeeded();
        if (atEnd()) {
            return 0;
        }
        return Math.min(getRemaining(), inputStream.available());
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    /**
     * If we haven't gotten to our start position yet, skip there!
     * @throws IOException if there's an issue with the underlying stream.
     * @throws IllegalStateException if it can't skip forward at all.
     */
    private void startIfNeeded() throws IOException {
        if (!haveDoneInitialSkip) {
            // The InputStream.skip() documentation says that the stream
            // can skip less than the requested number of bytes if it
            // wants to and EOF is only one of the possible reasons.
            // that's why we have a loop.
            while (iPosition < start) {
                long actuallySkipped = inputStream.skip(start-iPosition);
                iPosition += actuallySkipped;
                if (actuallySkipped == 0) {
                    // if we didn't make *any* progress at all skipping,
                    // let's hope we're at EOF.  if we're not, then we
                    // need to learn more about the type of input stream
                    // that's causing the issue and try to figure out
                    // if we can work around it.
                    B2Preconditions.checkState(inputStream.read() == EOF,
                            "made no progress skipping and not at end of stream? " +
                                    "inputStream is a " + inputStream.getClass().getCanonicalName());

                    // change our idea of the end of the stream.
                    pastEnd = iPosition;
                    break;
                }
            }

            // yep.  if an exception is thrown above, we don't claim we're done,
            // but if an exception is thrown, all bets are off anyway!
            haveDoneInitialSkip = true;
        }
    }

    private boolean atEnd() {
        return iPosition >= pastEnd;
    }

    private int getRemaining() {
        B2Preconditions.checkState(iPosition < pastEnd);
        return (int) (pastEnd - iPosition);
    }

    public long getExcerptStart() {
        return start;
    }

    public long getExcerptLength() {
        return pastEnd - start;
    }
}
