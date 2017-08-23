/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

/**
 * Use an instance of B2Sleeper to sleep.
 */
class B2Sleeper {

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
     * Tries to sleep for the specified amount of time.  If it gets interrupted,
     * it re-interrupts the current thread and returns (likely before the specified
     * time passes).
     *
     * @param seconds how long to try to sleep in seconds
     * @return true iff we slept the whole time without being interrupted.
     *         otherwise, interrupts the current thread and returns false.
     */
    @SuppressWarnings("UnusedReturnValue")
    boolean sleepSeconds(int seconds) {
        return sleepMilliseconds(seconds * 1000);
    }
}
