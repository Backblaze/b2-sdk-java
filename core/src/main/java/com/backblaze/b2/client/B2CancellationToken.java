/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Token to pass around whether the caller has cancelled an operation so that
 * sub tasks can stop their processing.
 */
public class B2CancellationToken {
    /**
     * The actual cancellation state
     */
    private AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Throws an exception if cancelled
     * @throws B2Exception
     */
    void throwIfCancelled() throws B2Exception {
        if (cancelled.get()) {
            throw new B2LocalException("cancelled", "Request cancelled by caller");
        }
    }

    /**
     * Sets the cancelled state
     */
    void cancel() {
        cancelled.set(true);
    }

    /**
     * Check if the state is cancelled
     * @return whether cancel has been called already
     */
    boolean isCancelled() {
        return cancelled.get();
    }
}
