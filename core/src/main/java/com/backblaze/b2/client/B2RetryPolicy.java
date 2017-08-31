/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;

/**
 * The B2RetryPolicy is called once after each attempt.  It is always passed
 * the number of attempts that have been made so far (attemptsSoFar) and
 * the number of milliseconds the call took (tookMs).  For unsuccessful
 * attempts, it is also passed the exception which caused the failure.
 *
 * For each of the getRetryable*() methods, the policy is consulted to decide
 * whether (and when) to retry.
 *
 * By the way, attemptsSoFar starts at 1.
 */
public interface B2RetryPolicy {
    /**
     * Called when callable.call() returns without an exception.
     *
     * @param operation the name of what is being retried.  *usually* the name of a b2 operation.
     * @param attemptsSoFar how many times have we called callable.call() so far?
     * @param tookMsecs     how long did this attempt take?
     */
    @SuppressWarnings("unused")
    default void succeeded(String operation,
                           int attemptsSoFar,
                           long tookMsecs) {
    }

    /**
     * Callable.call() threw a retryable B2Exception but we have to wait a
     * while before retrying.  If the guide decides we've tried enough times,
     * it should return null.  Otherwise, it should return the number of
     * seconds to sleep before trying again.
     * <p>
     * if e.getRetryAfterSecondsOrNull() is not null, it's the number of seconds
     * the server suggests that you wait before trying again.
     * <p>
     * WARNING: if we only hit retryable errors and this never returns null,
     * we will keep retrying indefinitely.  You have been warned.
     *
     * @param operation the name of what is being retried.  *usually* the name of a b2 operation.
     * @param attemptsSoFar how many times have we called callable.call() so far?
     * @param tookMsecs     how long did this attempt take?
     * @param e             the retryable exception.
     * @return null to stop trying OR the number of seconds to sleep before trying again.
     */
    Integer gotRetryableAfterDelay(String operation,
                                   int attemptsSoFar,
                                   long tookMsecs,
                                   B2Exception e);

    /**
     * Callable.call() threw a retryable B2Exception.  We will retry immediately.
     *
     * @param operation the name of what is being retried.  *usually* the name of a b2 operation.
     * @param attemptsSoFar how many times have we called callable.call() so far?
     * @param tookMsecs     how long did this attempt take?
     * @param e             the retryable exception.
     * @return true iff we should try again.
     */
    boolean gotRetryableImmediately(String operation,
                                    int attemptsSoFar,
                                    long tookMsecs,
                                    B2Exception e);

    /**
     * Callable.call() threw an unretryable B2Exception.  No more attempts will be made.
     *
     * @param operation the name of what is being retried.  *usually* the name of a b2 operation.
     * @param attemptsSoFar how many times have we called callable.call() so far?
     * @param tookMsecs     how long did this attempt take?
     * @param e             the unretryable exception.
     */
    @SuppressWarnings("unused")
    default void gotUnretryable(String operation,
                                int attemptsSoFar,
                                long tookMsecs,
                                B2Exception e) {

    }

    /**
     * Callable.call() threw an Exception that wasn't a B2Exception.  No more
     * attempts will be made.
     *
     * @param operation the name of what is being retried.  *usually* the name of a b2 operation.
     * @param attemptsSoFar how many times have we called callable.call() so far?
     * @param tookMsecs     how long did this attempt take?
     * @param e             the unexpected, unretryable exception.
     */
    default void gotUnexpectedUnretryable(String operation,
                                          int attemptsSoFar,
                                          long tookMsecs,
                                          Exception e) {
    }
}
