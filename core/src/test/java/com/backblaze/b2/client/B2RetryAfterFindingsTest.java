/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2ServiceUnavailableException;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class B2RetryAfterFindingsTest extends B2BaseTest {
    @After
    public void clearFlag() {
        Thread.interrupted();
    }

    private String runWithRetryAfter(Integer retryAfter) throws B2Exception {
        final AtomicInteger calls = new AtomicInteger();
        return new B2Retryer(new B2Sleeper()).doRetry("op", mock(B2AccountAuthorizationCache.class), (isRetry) -> {
            if (calls.getAndIncrement() == 0) {
                throw new B2ServiceUnavailableException("busy", retryAfter, "try later");
            }
            return "ok";
        }, B2DefaultRetryPolicy.supplier().get());
    }

    /** Finding C11: a negative Retry-After must not escape the retry loop as a raw IllegalArgumentException. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC11_negativeRetryAfterDoesNotThrowRawError() throws B2Exception {
        try {
            assertEquals("ok", runWithRetryAfter(-5));
        } catch (IllegalArgumentException e) {
            fail("raw error escaped the retry loop: " + e);
        }
    }

    /**
     * Finding C11: a very large Retry-After (here 2.2 million seconds) overflows the millisecond conversion.
     * The thread is pre-interrupted so that a correct implementation returns immediately instead of sleeping.
     */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC11_hugeRetryAfterDoesNotOverflowIntoRawError() {
        Thread.currentThread().interrupt();
        try {
            new B2Sleeper().sleepSeconds(2_200_000);
        } catch (IllegalArgumentException e) {
            fail("sleepSeconds overflowed into a raw error: " + e);
        }
    }

    /** Guard for C12 (judged not a bug): the default policy deliberately obeys the server's Retry-After without capping it. */
    @Test
    public void testGuardC12_defaultPolicyObeysServerRetryAfterWithoutCap() {
        final B2RetryPolicy policy = B2DefaultRetryPolicy.supplier().get();
        final Integer wait = policy.gotRetryableAfterDelay("op", 1, 0,
                new B2ServiceUnavailableException("busy", 86400, "try later"));
        assertEquals(Integer.valueOf(86400), wait);
    }
}
