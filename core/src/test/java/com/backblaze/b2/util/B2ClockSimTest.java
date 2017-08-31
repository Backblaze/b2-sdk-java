/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;

import static com.backblaze.b2.util.B2DateTimeUtil.ONE_SECOND_IN_MSECS;
import static com.backblaze.b2.util.B2DateTimeUtil.ONE_SECOND_IN_NANOS;
import static org.junit.Assert.assertEquals;

public class B2ClockSimTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void test() {
        // test create
        final B2ClockSim clock = new B2ClockSim(B2DateTimeUtil.EPOCH_TIME.plus(Duration.ofSeconds(6)));
        assertEquals(6 * ONE_SECOND_IN_MSECS, clock.getNowMsecTime());
        assertEquals(0L, clock.getMonoNanoTime());
        assertEquals(0L, clock.getMonoMsecTime());

        // test advanceBoth.  it should advance both types of time.
        clock.advanceBoth(Duration.ofMinutes(1));
        assertEquals((60 + 6) * ONE_SECOND_IN_MSECS, clock.getNowMsecTime());
        assertEquals(60 * ONE_SECOND_IN_NANOS, clock.getMonoNanoTime());
        assertEquals(60 * ONE_SECOND_IN_MSECS, clock.getMonoMsecTime());

        // test advanceNow.  it should only advance the wall-clock time.
        clock.advanceNow(Duration.ofHours(1));
        assertEquals((3600 + 60 + 6) * ONE_SECOND_IN_MSECS, clock.getNowMsecTime());
        assertEquals(60 * ONE_SECOND_IN_NANOS, clock.getMonoNanoTime());
        assertEquals(60 * ONE_SECOND_IN_MSECS, clock.getMonoMsecTime());
    }

    @Test
    public void testAdvanceWithNonPositiveDurations() {
        final B2ClockSim clock = new B2ClockSim(B2DateTimeUtil.EPOCH_TIME.plus(Duration.ofSeconds(6)));

        // adding a zero duration should be fine.
        clock.advanceBoth(Duration.ZERO);
        clock.advanceNow(Duration.ZERO);

        // advancing now by a negative time should be fine.
        clock.advanceNow(Duration.ofSeconds(-1));
        assertEquals(5 * ONE_SECOND_IN_MSECS, clock.getNowMsecTime());


        // advancingBoth by a negative time should throw.
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("delta must be non-negative");
        clock.advanceBoth(Duration.ofSeconds(-1));
    }
}