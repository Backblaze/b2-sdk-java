/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

/**
 * This listener is called as data is read from an input stream.
 * It is called synchronously before the caller receives the data from the stream.
 *
 * (1) keep in mind that the time you spend listening is directly slowing down
 *     the processing of the stream.
 *
 * (2) keep in mind that you're learning about things before the stream's caller.
 *     In particular, if you're using this as a proxy for the amount of data that's
 *     been sent to server, remember that it's not really a measure of that.  Many
 *     things can happen before the data is sent (and being sent isn't the same
 *     as being processed by the server!).  Higher-level indications (like receiving
 *     the response from the server) are better indications that the server has
 *     processed your data!
 */
public interface B2ByteProgressListener {
    /**
     * Called right before returning bytes to the caller.
     * @param nBytesSoFar the total number of bytes read from the stream so far.
     */
    void progress(long nBytesSoFar);

    /**
     * Called right before the exception propagates to the caller.
     * @param e the exception that was thrown
     * @param nBytesSoFar the total number of bytes read from the stream so far.
     */
    void hitException(Exception e, long nBytesSoFar);

    /**
     * Called right before the caller learns that we've hit the end of the stream.
     * @param nBytesSoFar the total number of bytes read from the stream so far.
     */
    void reachedEof(long nBytesSoFar);
}
