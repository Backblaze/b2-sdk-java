/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2BadRequestException;
import com.backblaze.b2.client.exceptions.B2ConnectFailedException;
import com.backblaze.b2.client.exceptions.B2ConnectionBrokenException;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2ForbiddenException;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.exceptions.B2NetworkTimeoutException;
import com.backblaze.b2.client.exceptions.B2RequestTimeoutException;
import com.backblaze.b2.client.exceptions.B2ServiceUnavailableException;
import com.backblaze.b2.client.exceptions.B2TooManyRequestsException;
import com.backblaze.b2.client.exceptions.B2UnauthorizedException;
import com.backblaze.b2.client.exceptions.B2UnauthorizedException.RequestCategory;
import com.backblaze.b2.util.B2Preconditions;
import org.junit.Test;

import static com.backblaze.b2.client.B2TestHelpers.makeAuth;
import static com.backblaze.b2.client.exceptions.B2UnauthorizedException.RequestCategory.ACCOUNT_AUTHORIZATION;
import static com.backblaze.b2.client.exceptions.B2UnauthorizedException.RequestCategory.OTHER;
import static com.backblaze.b2.client.exceptions.B2UnauthorizedException.RequestCategory.UPLOADING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class B2RetryerTest {
    // we need to mock this so we don't really sleep.
    private final B2Sleeper sleeper = mock(B2Sleeper.class);

    private final B2RetryPolicy policy = mock(B2RetryPolicy.class);

    // this authCache always returns a good auth when it's asked for one.
    // used primarily to check whether clear() was called.
    private final B2AccountAuthorizationCache goodAuthCache;

    private final B2Retryer retryer = new B2Retryer(sleeper);


    private static class Guts implements B2Retryer.RetryableCallable<String> {
        private final Object[] results;
        private int iCurrentResult;

        private Guts(Object... results) {
            this.results = results;
        }

        int getCallCount() {
            return iCurrentResult;
        }

        @Override
        public String call(boolean isRetry) throws Exception {
            B2Preconditions.checkState(iCurrentResult < results.length, "called too many times?");
            B2Preconditions.checkArgument(isRetry == (iCurrentResult != 0));

            Object result = results[iCurrentResult];
            iCurrentResult++;

            if (result instanceof Exception) {
                throw (Exception) result;
            } else if (result instanceof String) {
                return (String) result;
            } else {
                throw new RuntimeException("unexpected result object type: " + result);
            }
        }

        @SuppressWarnings("SameParameterValue")
        B2Exception getAsException(int i) {
            final Object result = results[i];
            B2Preconditions.checkArgument(result instanceof B2Exception);
            return (B2Exception) result;
        }
    }

    public B2RetryerTest() throws B2Exception {
        goodAuthCache = mock(B2AccountAuthorizationCache.class);
        doReturn(makeAuth(1)).when(goodAuthCache).get();
    }

    @Test
    public void testFirstTimeSuccess() throws B2Exception {
        assertEquals("one", retryer.doRetry(goodAuthCache, () -> "one", policy));
        verify(goodAuthCache, never()).clear();
        verify(policy, times(1)).succeeded(eq(1), anyLong());
        verifyNoMoreInteractions(policy);
    }

    @Test
    public void testUnauthInAccountAuthorizationIsNotRetryable() throws B2Exception {
        final Guts guts = new Guts(unauthorized(ACCOUNT_AUTHORIZATION));

        boolean caughtIt = false;
        try {
            retryer.doRetry(goodAuthCache, guts, policy);
        } catch (B2UnauthorizedException e) {
            assertEquals(ACCOUNT_AUTHORIZATION, e.getRequestCategory());
            assertEquals(1, guts.getCallCount());
            verify(goodAuthCache, never()).clear();
            verify(policy, times(1)).gotUnretryable(eq(1), anyLong(), any(B2UnauthorizedException.class));
            verifyNoMoreInteractions(policy);
            caughtIt = true;
        }
        assertTrue(caughtIt);
    }

    @Test
    public void testAuthThrowsRetryableUnauthSeveralTimesButWorksBeforeWeGiveUp() throws B2Exception {
        when(policy.gotRetryableImmediately(anyInt(), anyLong(), any())).thenReturn(true);

        // this throws a B2UnauthorizedException until the last attempt, when it replies.
        final Guts guts = new Guts(
                unauthorized(OTHER),
                unauthorized(OTHER),
                unauthorized(OTHER),
                unauthorized(OTHER),
                unauthorized(OTHER),
                unauthorized(OTHER),
                "hello"
                );

        assertEquals("hello", retryer.doRetry(goodAuthCache, guts, policy));
        assertEquals(7, guts.getCallCount());

        verify(goodAuthCache, times(6)).clear();
        verify(sleeper, never()).sleepSeconds(anyInt());
        verify(policy, times(6)).gotRetryableImmediately(anyInt(), anyLong(), any(B2UnauthorizedException.class));
    }

    @Test
    public void testAuthThrowsRetryableUnauthTooManyTimesAndWeGiveUp() throws B2Exception {
        when(policy.gotRetryableImmediately(anyInt(), anyLong(), any())).thenReturn(true, true, true, true, true, true, true, false);

        final Guts guts = new Guts(
                unauthorized(OTHER),
                unauthorized(UPLOADING),
                unauthorized(OTHER),
                unauthorized(UPLOADING),
                unauthorized(UPLOADING),
                unauthorized(OTHER),
                unauthorized(OTHER),
                unauthorized(OTHER),
                "this result won't be used"
                );

        boolean caughtIt = false;
        try {
            retryer.doRetry(goodAuthCache, guts, policy);
        } catch (B2UnauthorizedException e) {
            assertTrue(guts.getAsException(7) == e);
            assertEquals(8, guts.getCallCount());
            verify(goodAuthCache, times(5)).clear(); // once for each OTHER.
            verify(sleeper, never()).sleepSeconds(anyInt());
            verify(policy, times(8)).gotRetryableImmediately(anyInt(), anyLong(), any());
            verifyNoMoreInteractions(policy);
            caughtIt = true;
        }
        assertTrue(caughtIt);
    }

    @Test
    public void testRetryableAfterDelayFinallyWorks() throws B2Exception {
        final Guts guts = new Guts(
                tooManyRequests(null),
                serviceUnavailable(null),
                requestTimeout(null),
                requestTimeout(null),
                tooManyRequests(null),
                serviceUnavailable(null),
                "yippee"
        );

        when(policy.gotRetryableAfterDelay(anyInt(), anyLong(), any())).thenReturn(3, 5, 7, 11, 13, 17);
        assertEquals("yippee", retryer.doRetry(goodAuthCache, guts, policy));

        assertEquals(7, guts.getCallCount());
        verify(goodAuthCache, never()).clear();

        // this verifies that we use the answer from the policy to sleep.
        verify(sleeper, times(1)).sleepSeconds(3);
        verify(sleeper, times(1)).sleepSeconds(5);
        verify(sleeper, times(1)).sleepSeconds(7);
        verify(sleeper, times(1)).sleepSeconds(11);
        verify(sleeper, times(1)).sleepSeconds(13);
        verify(sleeper, times(1)).sleepSeconds(17);
        verifyNoMoreInteractions(sleeper);
    }

    @Test
    public void testExponentialBackoffForRetryable_andStopEventuallyBecauseItNeverWorks() throws B2Exception {
        final Guts guts = new Guts(
                tooManyRequests(null),
                serviceUnavailable(null),
                internalError(),
                requestTimeout(null),
                tooManyRequests(null),
                connectFailed(),
                connectionBroken(),
                socketTimeout()
        );

        boolean caughtIt = false;
        try {
            retryer.doRetry(goodAuthCache, guts, new B2DefaultRetryPolicy());
        } catch (B2Exception e) {
            assertEquals(8, guts.getCallCount());
            assertTrue(e == guts.getAsException(7));
            verify(goodAuthCache, never()).clear();

            // we're using the default policy here.  it doubles the backoff and stops after a while.
            // this verifies that if the policy says to stop retrying, we do.
            verify(sleeper, times(1)).sleepSeconds(1);
            verify(sleeper, times(1)).sleepSeconds(2);
            verify(sleeper, times(1)).sleepSeconds(4);
            verify(sleeper, times(1)).sleepSeconds(8);
            verify(sleeper, times(1)).sleepSeconds(16);
            verify(sleeper, times(1)).sleepSeconds(32);
            verify(sleeper, times(1)).sleepSeconds(64);
            verifyNoMoreInteractions(sleeper);

            caughtIt = true;
        }
        assertTrue(caughtIt);
    }

    @Test
    public void testOtherExceptionsArentRetried() {
        checkNotRetried(badRequest());
        checkNotRetried(forbidden());
        checkNotRetried(baseException());
        checkNotRetried(new RuntimeException("testing"));
    }

    private void checkNotRetried(Exception exceptionToThrowFromCallable) {
        final Guts guts = new Guts(exceptionToThrowFromCallable);

        boolean caughtIt = false;
        try {
            retryer.doRetry(goodAuthCache, guts, policy);
        } catch (B2Exception | RuntimeException e) {
            assertEquals(1, guts.getCallCount());
            if (exceptionToThrowFromCallable instanceof B2Exception) {
                assertTrue(e == exceptionToThrowFromCallable);
            } else {
                assertTrue(e.getCause() == exceptionToThrowFromCallable);
                assertTrue(e.getMessage().startsWith("unexpected"));
            }
            verifyNoMoreInteractions(goodAuthCache);
            verifyNoMoreInteractions(sleeper);

            if (exceptionToThrowFromCallable instanceof B2Exception) {
                verify(policy, times(1)).gotUnretryable(eq(1), anyLong(), eq((B2Exception) exceptionToThrowFromCallable));
            } else {
                verify(policy, times(1)).gotUnexpectedUnretryable(eq(1), anyLong(), eq(exceptionToThrowFromCallable));
            }
            verifyNoMoreInteractions(policy);

            caughtIt = true;
        }
        assertTrue(caughtIt);
    }

    private B2UnauthorizedException unauthorized(RequestCategory category) {
        final B2UnauthorizedException e = new B2UnauthorizedException("test", null, "message");
        e.setRequestCategory(category);
        return e;
    }

    @SuppressWarnings("SameParameterValue")
    private B2TooManyRequestsException tooManyRequests(Integer retryAfterSecsOrNull) {
        return new B2TooManyRequestsException("test", retryAfterSecsOrNull, "message");
    }
    @SuppressWarnings("SameParameterValue")
    private B2ServiceUnavailableException serviceUnavailable(Integer retryAfterSecsOrNull) {
        return new B2ServiceUnavailableException("test", retryAfterSecsOrNull, "message");
    }
    @SuppressWarnings("SameParameterValue")
    private B2RequestTimeoutException requestTimeout(Integer retryAfterSecsOrNull) {
        return new B2RequestTimeoutException("test", retryAfterSecsOrNull, "message");
    }
    private B2BadRequestException badRequest() {
        return new B2BadRequestException("test", null, "message");
    }
    private B2ForbiddenException forbidden() {
        return new B2ForbiddenException("test", null, "message");
    }
    private B2InternalErrorException internalError() {
        return new B2InternalErrorException("test", null, "message");
    }
    private B2ConnectFailedException connectFailed() {
        return new B2ConnectFailedException("test", null, "message");
    }
    private B2ConnectionBrokenException connectionBroken() {
        return new B2ConnectionBrokenException("test", null, "message");
    }
    private B2NetworkTimeoutException socketTimeout() {
        return new B2NetworkTimeoutException("test", null, "message");
    }
    private B2Exception baseException() {
        return new B2Exception("test", 666, null, "message");
    }

}
