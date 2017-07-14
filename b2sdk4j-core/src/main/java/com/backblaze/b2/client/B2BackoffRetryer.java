/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.exceptions.B2NetworkBaseException;
import com.backblaze.b2.client.exceptions.B2RequestTimeoutException;
import com.backblaze.b2.client.exceptions.B2ServiceUnavailableException;
import com.backblaze.b2.client.exceptions.B2TooManyRequestsException;
import com.backblaze.b2.client.exceptions.B2UnauthorizedException;

import java.util.concurrent.Callable;

/**
 * Instances of this class provide helpers to do automatic backoff.
 *
 * XXX: might not want this for interactive applications.  It can get really slow.
 */
class B2BackoffRetryer {
    // MAX_ATTEMPTS is the largest number of times we're willing to try a call, including the original attempt.
    // our documentation proposes backing off up to 64 seconds, so that's how i picked 8.
    //   attempt#1, sleepSecs(1), attempt#2, sleepSecs(2), attempt#3, sleep(Secs4),
    //   attempt#4, sleepSecs(8), attempt#5, sleepSecs(16), attempt#6, sleepSecs(32),
    //   attempt#7, sleepSecs(64),
    //   attempt#8
    // XXX: probably come up with a custom retry policy interface (eventually?)
    private static final int MAX_ATTEMPTS = 8;

    private final B2Sleeper sleeper;

    B2BackoffRetryer(B2Sleeper sleeper) {
        this.sleeper = sleeper;
    }

    interface RetryableCallable<T> {
        T call(boolean isRetry) throws Exception;
    }

    /**
     * Just like doRetry(B2AccountAuthorizationCache, RetryableCallable) except
     * that it takes a normal callable instead of a RetryableCallable.
     *
     * You are encouraged to use this version because if your code's behaviour
     * doesn't change based on whether it's a retry, there's less for you to test.
     */
    <T> T doRetry(B2AccountAuthorizationCache accountAuthCache,
                  Callable<T> callable) throws B2Exception {
        return doRetry(accountAuthCache, isRetry -> callable.call());
    }

    /**
     * This retries the given callable until one of these happens:
     * * it returns cleanly with no exception
     * * it throws a non-retryable error
     * * it throws a retryable-error, but we've already retried too many times.
     * <p>
     * It clears the accountAuthorizationCache as needed.
     *
     * @param accountAuthCache will be cleared as needed.
     * @param callable         the code to run (and retry as needed)
     * @return whatever is returned by callable.call().
     * @throws B2Exception if there's any non-retryable error,
     *                     or the last retryable error if we
     *                     run out of retries.
     */
    <T> T doRetry(B2AccountAuthorizationCache accountAuthCache,
                  RetryableCallable<T> callable) throws B2Exception {
        B2Exception mostRecentException = null;

        int waitBetweenRetrySecs = 1;

        for (int iAttempt = 0; iAttempt < MAX_ATTEMPTS; iAttempt++) {
            try {
                try {
                    final boolean isRetry = (iAttempt != 0);
                    return callable.call(isRetry);
                } catch (B2Exception e) {
                    mostRecentException = e;
                    throw e;
                }
            } catch (B2UnauthorizedException e) {
                switch (e.getRequestCategory()) {
                    case ACCOUNT_AUTHORIZATION:
                        // unauthorized during account authorization is NOT retryable.
                        throw e;

                    case UPLOADING:
                        // nothing to do.  the upload url won't have been returned to the
                        // pool, so it won't be reused.  we'll try again with another url.
                        // (in fact, the B2UploadUrlCache will always get a new URL for retries.)
                        break;

                    case OTHER:
                        accountAuthCache.clear();
                        break;
                }
            } catch (B2TooManyRequestsException |
                    B2ServiceUnavailableException |
                    B2InternalErrorException |
                    B2RequestTimeoutException |
                    B2NetworkBaseException e) {
                if (iAttempt < (MAX_ATTEMPTS - 1)) {
                    if (e.getRetryAfterSecondsOrNull() != null) {
                        // the server specified an amount of time to wait, so let's obey.
                        sleeper.sleepSecondsOrThrow(e.getRetryAfterSecondsOrNull(), mostRecentException);

                        // and our docs say we should reset to default to a 1-second
                        // timeout after getting a retry-after.
                        waitBetweenRetrySecs = 1;
                    } else {
                        // sleep based on our current backoff amount.
                        sleeper.sleepSecondsOrThrow(waitBetweenRetrySecs, mostRecentException);

                        // double the default waiting time for the next attempt, if any.
                        waitBetweenRetrySecs *= 2;
                    }
                }
            } catch (B2Exception e) {
                // other types of exceptions aren't retryable!
                throw e;
            } catch (Exception e) {
                throw new B2Exception("unexpected", 500, null, "unexpected: " + e, e);
            }
        }

        // i haven't convinced myself that making a special "too many retries"
        // exception to hold the underlying cause is sufficiently useful, so
        // let's throw the most recent exception we got.  our documentation
        // already says that if the caller gets a retryable exception, we've
        // already retried it.
        throw mostRecentException;
    }
}