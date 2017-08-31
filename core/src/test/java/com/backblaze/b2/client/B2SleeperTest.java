/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.util.B2Clock;
import com.backblaze.b2.util.B2ClockImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

public class B2SleeperTest {
    private final B2Sleeper sleeper = new B2Sleeper();

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testSleeping() {
        final B2Clock clock = new B2ClockImpl();
        final long beforeMillis = clock.monotonicMillis();
        sleeper.sleepSeconds(1);
        final long afterMillis = clock.monotonicMillis();

        // we should've slept most of a second! (and not thrown)
        assertTrue((afterMillis - beforeMillis) > 900);
    }

    @Test
    public void testInterruptedSleep() {
        // interrupt this thread, so sleeping will throw.
        Thread.currentThread().interrupt();

        // try to sleep.  it shouldn't throw...
        sleeper.sleepSeconds(1);

        // ...and the thread should still be flagged as interrupted.
        assertTrue(Thread.interrupted()); // this checks & clears the flag.
    }
}
