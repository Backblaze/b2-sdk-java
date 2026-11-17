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

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class B2RetryerFindingsTest extends B2BaseTest {
    @After
    public void clearFlag() {
        Thread.interrupted();
    }

    /**
     * Finding C10: when the thread's interrupt flag is set (for example after a cancelled upload task),
     * the retry loop must not burn through all its attempts with no pause at all: it should stop
     * retrying (or still pause).
     */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC10_interruptedThreadDoesNotRetryWithoutPausing() {
        final B2Retryer retryer = new B2Retryer(new B2Sleeper());
        final AtomicInteger attempts = new AtomicInteger();
        final long start = System.nanoTime();
        Thread.currentThread().interrupt();
        try {
            retryer.doRetry("op", mock(), (isRetry) -> {
                attempts.incrementAndGet();
                throw new B2InternalErrorException("always failing");
            }, B2DefaultRetryPolicy.supplier().get());
        } catch (B2Exception expected) {
            // expected
        }
        final long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        assertTrue("made " + attempts.get() + " attempts in " + elapsedMillis + " ms with no pause",
                attempts.get() <= 2 || elapsedMillis >= 1000);
    }

    private static B2AccountAuthorizationCache mock() {
        return org.mockito.Mockito.mock(B2AccountAuthorizationCache.class);
    }
}
