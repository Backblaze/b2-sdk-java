/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HttpClientFactoryFindingsTest {
    /** Finding C17: the idle-connection cleaner thread must be a daemon so it cannot keep the JVM alive. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC17_idleConnectionMonitorThreadIsDaemon() {
        final HttpClientFactoryImpl factory = HttpClientFactoryImpl.build();
        try {
            Thread monitor = null;
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getClass().getName().contains("IdleConnectionMonitorThread")) {
                    monitor = t;
                }
            }
            assertNotNull("cleaner thread not found", monitor);
            assertTrue("cleaner thread is not a daemon", monitor.isDaemon());
        } finally {
            factory.close();
        }
    }

    /**
     * Guard for C18 (judged not a bug): close() must return promptly (it should not sit out the 5 s wait).
     * The cleaner thread is first allowed to reach its wait, so the guard is deterministic: calling close()
     * in the instant between thread start and its first wait() is a separate startup race, which would make
     * this guard fail intermittently if it were not excluded here.
     */
    @Test
    public void testGuardC18_closeReturnsPromptly() {
        for (int i = 0; i < 5; i++) {
            final HttpClientFactoryImpl factory = HttpClientFactoryImpl.build();
            awaitCleanerThreadWaiting();
            final long start = System.nanoTime();
            factory.close();
            final long ms = (System.nanoTime() - start) / 1_000_000;
            assertTrue("close() took " + ms + " ms", ms < 4000);
        }
    }

    /** Waits (bounded) until a cleaner thread is parked in wait(), so close() cannot race its startup. */
    private static void awaitCleanerThreadWaiting() {
        final long deadline = System.nanoTime() + 2_000_000_000L;
        while (System.nanoTime() < deadline) {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getClass().getName().contains("IdleConnectionMonitorThread")
                        && t.getState() == Thread.State.TIMED_WAITING) {
                    return;
                }
            }
            Thread.yield();
        }
    }
}
