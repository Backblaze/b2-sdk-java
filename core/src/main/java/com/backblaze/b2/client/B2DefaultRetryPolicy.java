/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;

import java.util.function.Supplier;

/**
 * B2DefaultRetryPolicy implements the retry policy described in the B2
 * documentation.  It should be reasonable and sufficient for almost every
 * use of B2.
 *
 * Each attempted operation should have a unique instance of this class
 * because it stores state about retries between calls.  Use an instance
 * of Supplier&lt;B2RetryPolicy&gt; which provides a new instance on each call.
 */
public class B2DefaultRetryPolicy implements B2RetryPolicy {
    // MAX_ATTEMPTS is the largest number of times we're willing to try a call, including the original attempt.
    // our documentation proposes backing off up to 64 seconds, so that's how i picked 8.
    //   attempt#1, sleepSecs(1), attempt#2, sleepSecs(2), attempt#3, sleep(Secs4),
    //   attempt#4, sleepSecs(8), attempt#5, sleepSecs(16), attempt#6, sleepSecs(32),
    //   attempt#7, sleepSecs(64),
    //   attempt#8
    private static final int MAX_ATTEMPTS = 8;

    private int waitBetweenRetrySecs = 1;

    /**
     * @return a supplier to create new instances of this class.
     */
    public static Supplier<B2RetryPolicy> supplier() {
        return B2DefaultRetryPolicy::new;
    }

    @Override
    public Integer gotRetryableAfterDelay(String operation,
                                          int attemptsSoFar,
                                          long tookMillis,
                                          B2Exception e) {
        if (attemptsSoFar < MAX_ATTEMPTS) {
            final Integer secsFromServer = e.getRetryAfterSecondsOrNull();
            if (secsFromServer != null) {
                // the server specified an amount of time to wait, so let's obey.
                // and our docs say we should reset to default to a 1-second
                // timeout after getting a retry-after.
                waitBetweenRetrySecs = 1;

                return secsFromServer;
            } else {
                // we'll sleep based on the current retry amount.
                final int secsToSleep = waitBetweenRetrySecs;

                // double the default waiting time for the next attempt, if any.
                waitBetweenRetrySecs *= 2;

                return secsToSleep;
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean gotRetryableImmediately(String operation,
                                           int attemptsSoFar,
                                           long tookMillis,
                                           B2Exception e) {
        return (attemptsSoFar < MAX_ATTEMPTS);
    }
}
