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
     * Guard for C18 (judged not a bug): close() must return promptly (it should not sit out the 5 s wait),
     * even when called right after build().  Note: the defect, if present, is a race, so this guard can pass
     * while the race exists; it cannot produce a false failure.
     */
    @Test
    public void testGuardC18_closeReturnsPromptly() {
        for (int i = 0; i < 5; i++) {
            final HttpClientFactoryImpl factory = HttpClientFactoryImpl.build();
            final long start = System.nanoTime();
            factory.close();
            final long ms = (System.nanoTime() - start) / 1_000_000;
            assertTrue("close() took " + ms + " ms", ms < 4000);
        }
    }
}
