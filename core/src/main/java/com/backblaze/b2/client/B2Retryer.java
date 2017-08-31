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
import com.backblaze.b2.util.B2Clock;

import java.util.concurrent.Callable;

/**
 * Instances of this class provide helpers to do automatic backoff and retrying
 * for retryable errors.  The backoff behavior is determined by the B2RetryPolicy
 * that are passed in.
 */
class B2Retryer {
    private final B2Sleeper sleeper;

    B2Retryer(B2Sleeper sleeper) {
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
    <T> T doRetry(String operation,
                  B2AccountAuthorizationCache accountAuthCache,
                  Callable<T> callable,
                  B2RetryPolicy retryPolicy) throws B2Exception {
        return doRetry(operation, accountAuthCache, isRetry -> callable.call(), retryPolicy);
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
     * @param retryPolicy called to determine how to handle retryable errors.
     * @return whatever is returned by callable.call().
     * @throws B2Exception if there's any non-retryable error,
     *                     or the last retryable error if we
     *                     run out of retries.
     */
    <T> T doRetry(String operation,
                  B2AccountAuthorizationCache accountAuthCache,
                  RetryableCallable<T> callable,
                  B2RetryPolicy retryPolicy) throws B2Exception {
        final B2Clock clock = B2Clock.get();

        // keeps trying until we hit an unretryable exception or the retryPolicy says to stop.
        int attemptsSoFar = 0; // we haven't attempted it at all yet.
        while (true) {
            final long beforeMonoMillis = clock.monotonicMillis();

            // i have to set this to a default value because clock.monotonicMillis()
            // in the finally block below could throw and then tookMillis wouldn't be
            // set in the catch(Exception) block way below.
            long tookMillis = -1;

            try {
                try {
                    final boolean isRetry = (attemptsSoFar != 0);
                    attemptsSoFar++; // about to attempt again.

                    final T value = callable.call(isRetry);
                    tookMillis = clock.monotonicMillis() - beforeMonoMillis;
                    retryPolicy.succeeded(operation, attemptsSoFar, tookMillis);

                    return value;
                } finally {
                    // be sure to set tookMillis for exception handling below.
                    tookMillis = clock.monotonicMillis() - beforeMonoMillis;
                }
            } catch (B2UnauthorizedException e) {
                switch (e.getRequestCategory()) {
                    case ACCOUNT_AUTHORIZATION:
                        // unauthorized during account authorization is NOT retryable.
                        retryPolicy.gotUnretryable(operation, attemptsSoFar, tookMillis, e);
                        throw e;

                    case UPLOADING:
                        // nothing to do.  the upload url won't have been returned to the
                        // pool, so it won't be reused.  we'll try again with another url.
                        // (in fact, the B2UploadUrlCache will always get a new URL for retries.)
                        if (!retryPolicy.gotRetryableImmediately(operation, attemptsSoFar, tookMillis, e)) {
                            throw e;
                        }
                        continue; // to go around the loop and try again.

                    case OTHER:
                        accountAuthCache.clear();
                        if (!retryPolicy.gotRetryableImmediately(operation, attemptsSoFar, tookMillis, e)) {
                            throw e;
                        }

                        //noinspection UnnecessaryContinue
                        continue; // to go around the loop and try again.
                }
            } catch (B2TooManyRequestsException |
                    B2ServiceUnavailableException |
                    B2InternalErrorException |
                    B2RequestTimeoutException |
                    B2NetworkBaseException e) {

                final Integer waitSeconds = retryPolicy.gotRetryableAfterDelay(operation, attemptsSoFar, tookMillis, e);
                if (waitSeconds == null) {
                    // i haven't convinced myself that making a special "too many retries"
                    // exception to hold the underlying cause is sufficiently useful, so
                    // let's throw the most recent exception we got.  our documentation
                    // already says that if the caller gets a retryable exception, we've
                    // already retried it.
                    throw e;
                }

                // this sleep might return early, but it won't throw.  if it gets interrupted
                // it resets the thread's interrupted flag so that we'll get the error later,
                // hopefully during the next attempt (which is probably blocking on something
                // like IO).  if we threw from here without retrying, we might surprise our
                // B2RetryPolicy since it just told us to try again and we wouldn't.
                sleeper.sleepSeconds(waitSeconds);
            } catch (B2Exception e) {
                // other types of exceptions aren't retryable!
                retryPolicy.gotUnretryable(operation, attemptsSoFar, tookMillis, e);
                throw e;
            } catch (Exception e) {
                // callable.call() throws Exception, so I have to catch Exception (shudder!).
                // i don't want to suppress an InterruptedException, so check for it here.
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt(); // reset the flag!
                }
                retryPolicy.gotUnexpectedUnretryable(operation, attemptsSoFar, tookMillis, e);
                throw new B2Exception("unexpected", 500, null, "unexpected: " + e, e);
            }
        }
    }


}