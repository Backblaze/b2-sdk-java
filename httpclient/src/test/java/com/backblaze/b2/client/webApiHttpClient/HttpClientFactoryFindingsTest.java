/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

public class HttpClientFactoryFindingsTest {
    /** The cleaner thread that belongs to this very factory (not some other factory's). */
    private static Thread cleanerThreadOf(HttpClientFactoryImpl factory) throws Exception {
        final Field f = HttpClientFactoryImpl.class.getDeclaredField("connectionJanitor");
        f.setAccessible(true);
        return (Thread) f.get(factory);
    }

    /** Finding C17: the idle-connection cleaner thread must be a daemon so it cannot keep the JVM alive. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC17_idleConnectionMonitorThreadIsDaemon() throws Exception {
        final HttpClientFactoryImpl factory = HttpClientFactoryImpl.build();
        try {
            final Thread monitor = cleanerThreadOf(factory);
            assertNotNull("cleaner thread not found", monitor);
            assertTrue("cleaner thread is not a daemon", monitor.isDaemon());
        } finally {
            factory.close();
        }
    }

    /**
     * Guard for C18 (judged not a bug): close() must return promptly (it should not sit out the 5 s wait).
     * This factory's own cleaner thread is first required to reach its wait, so the guard is deterministic:
     * calling close() in the instant between thread start and its first wait() is a separate startup race.
     * close() runs on a daemon thread and is given a bounded time, so a close() that hangs is reported as a
     * timeout instead of blocking the test run.
     */
    @Test
    public void testGuardC18_closeReturnsPromptly() throws Exception {
        final ExecutorService closer = Executors.newSingleThreadExecutor(r -> {
            final Thread t = new Thread(r, "C18-closer");
            t.setDaemon(true);
            return t;
        });
        try {
            for (int i = 0; i < 5; i++) {
                final HttpClientFactoryImpl factory = HttpClientFactoryImpl.build();
                boolean closeSubmitted = false;
                try {
                    awaitWaiting(cleanerThreadOf(factory));
                    final long start = System.nanoTime();
                    final Future<?> closing = closer.submit(factory::close);
                    closeSubmitted = true;
                    try {
                        closing.get(4, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        closing.cancel(true);
                        fail("close() did not return within 4 s");
                    }
                    final long ms = (System.nanoTime() - start) / 1_000_000;
                    assertTrue("close() took " + ms + " ms", ms < 4000);
                } finally {
                    if (!closeSubmitted) {
                        factory.close();
                    }
                }
            }
        } finally {
            closer.shutdownNow();
        }
    }

    /** Waits (bounded) until the given cleaner thread is parked in wait(); fails if it never gets there. */
    private static void awaitWaiting(Thread cleaner) {
        final long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (cleaner.getState() == Thread.State.TIMED_WAITING || cleaner.getState() == Thread.State.WAITING) {
                return;
            }
            Thread.yield();
        }
        fail("the factory's cleaner thread did not reach its wait; state=" + cleaner.getState());
    }
}
