/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class B2RetryerFindingsTest extends B2BaseTest {
    @After
    public void clearFlag() {
        Thread.interrupted();
    }

    /**
     * A sleeper that never really sleeps: it records each request and behaves like Thread.sleep() called on a
     * thread whose interrupt flag is set (the interrupted sleep returns at once, re-interrupts, reports false).
     */
    private static class InterruptedSleeper extends B2Sleeper {
        final List<Integer> requestedSeconds = new ArrayList<>();

        @Override
        boolean sleepSeconds(int seconds) {
            requestedSeconds.add(seconds);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Finding C10: when the thread's interrupt flag is set (for example after a cancelled upload task),
     * the retry loop must not burn through all its attempts with no pause at all: it should stop
     * retrying.  The sleeper is controlled by the test, so the result does not depend on wall-clock time.
     */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC10_interruptedThreadDoesNotRetryWithoutPausing() {
        final InterruptedSleeper sleeper = new InterruptedSleeper();
        final B2Retryer retryer = new B2Retryer(sleeper);
        final AtomicInteger attempts = new AtomicInteger();
        Thread.currentThread().interrupt();
        try {
            retryer.doRetry("op", mock(), (isRetry) -> {
                attempts.incrementAndGet();
                throw new B2InternalErrorException("always failing");
            }, B2DefaultRetryPolicy.supplier().get());
            fail("the operation always fails, so doRetry must throw");
        } catch (B2Exception expected) {
            // expected
        }
        assertTrue("the operation was never attempted", attempts.get() >= 1);
        assertTrue("made " + attempts.get() + " attempts although every pause (" + sleeper.requestedSeconds
                        + " s requested) was cut short by the interrupt",
                attempts.get() <= 2);
    }

    private static B2AccountAuthorizationCache mock() {
        return org.mockito.Mockito.mock(B2AccountAuthorizationCache.class);
    }
}
