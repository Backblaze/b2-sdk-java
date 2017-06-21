/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.util.B2Preconditions;

/**
 * Use an instance of B2Sleeper to sleep.
 */
public class B2Sleeper {

    /**
     * @param milliseconds how long to try to sleep in milliseconds
     * @return true iff we slept the whole time without being interrupted.
     *         otherwise, interrupts the current thread and returns false.
     */
    private boolean sleepMilliseconds(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * @param seconds how long to try to sleep in seconds
     * @return true iff we slept the whole time without being interrupted.
     *         otherwise, interrupts the current thread and returns false.
     */
    private boolean sleepSeconds(int seconds) {
        return sleepMilliseconds(seconds * 1000);
    }

    /**
     * Tries to sleep for the specified amount of time.  If it gets interrupted,
     * it re-interrupts the current thread, and throws the given exception.
     *
     * @param seconds how long to try to sleep for.
     * @param b2Exception the exception to throw if the sleeping gets interrupted.
     * @throws B2Exception if interrupted.
     */
    void sleepSecondsOrThrow(int seconds,
                             B2Exception b2Exception) throws B2Exception {
        B2Preconditions.checkArgument(b2Exception != null);
        if (!sleepSeconds(seconds)) {
            throw b2Exception;
        }
    }
}
