/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.util.B2ByteProgressListener;
import com.backblaze.b2.util.B2Clock;

import static com.backblaze.b2.util.B2DateTimeUtil.ONE_SECOND_IN_MILLIS;

/**
 * B2ByteProgressFilteringListener only forwards a subset of the calls to the
 * listener it wraps to minimize the amount of work being done.
 *
 * You can specify the minimum amount of time between times that calls to
 * progress() are forwarded to the wrapped listener.
 *
 * This does NOT guarantee that you will get updates that often.
 * If just means that progress() calls that come in sooner less
 * than that amount of time since the previously forwarded call will
 * not be forwarded.
 *
 * Calls to hitException() and reachedEof() are always forwarded,
 * independent of the time-based filtering.
 */
class B2ByteProgressFilteringListener implements B2ByteProgressListener {
    private final B2ByteProgressListener listener;
    private final long nMsecsBetween;

    // at what time should we send the next progress?
    private long msecsThreshold;

    // what's the most recent bytesSoFar we've seen?
    private long bytesSoFar;

    /**
     * @param listener the listener to forward notifications to.
     * @param nMsecsBetween if this much time has passed since previous progress() was forwarded, forward it.
     */
    private B2ByteProgressFilteringListener(B2ByteProgressListener listener,
                                            long nMsecsBetween) {
        this.listener = listener;
        this.nMsecsBetween = nMsecsBetween;
    }

    /**
     * @param listener the listener to forward notifications to with a default nMsecsBetween.
     */
    B2ByteProgressFilteringListener(B2ByteProgressListener listener) {
        this(listener, 5 * ONE_SECOND_IN_MILLIS);
    }

    @Override
    public void progress(long nBytesSoFar) {
        this.bytesSoFar = nBytesSoFar;

        final long monoMsecs = B2Clock.get().getMonoMsecTime();

        // only send if enough time has gone by.
        if (monoMsecs >= msecsThreshold) {
            listener.progress(nBytesSoFar);

            // reset threshold for next send.
            msecsThreshold = monoMsecs + nMsecsBetween;
        }
    }

    @Override
    public void hitException(Exception e,
                             long nBytesSoFar) {
        this.bytesSoFar = nBytesSoFar;
        listener.hitException(e, nBytesSoFar);
    }

    @Override
    public void reachedEof(long nBytesSoFar) {
        this.bytesSoFar = nBytesSoFar;
        listener.reachedEof(nBytesSoFar);
    }

    public long getBytesSoFar() {
        return bytesSoFar;
    }
}
