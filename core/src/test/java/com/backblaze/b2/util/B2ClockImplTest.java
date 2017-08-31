/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import org.junit.Test;

import static com.backblaze.b2.util.B2DateTimeUtil.ONE_SECOND_IN_NANOS;
import static org.junit.Assert.assertTrue;

public class B2ClockImplTest {

    @Test
    public void test() throws InterruptedException {
        final B2ClockImpl clock = new B2ClockImpl();

        final long beforeNowMsecs = clock.getNowMsecTime();
        final long beforeMonoMsecs = clock.getMonoMsecTime();
        final long beforeNanos = clock.getMonoNanoTime();

        Thread.sleep(1000);

        final long afterNowMsecs = clock.getNowMsecTime();
        final long afterMonoMsecs = clock.getMonoMsecTime();
        final long afterNanos = clock.getMonoNanoTime();

        final long deltaNowMsecs = afterNowMsecs - beforeNowMsecs;
        final long deltaMonoMsecs = afterMonoMsecs - beforeMonoMsecs;
        final long deltaNanos = afterNanos - beforeNanos;

        assertTrue("nowMsecs: after(" + afterNowMsecs + ") - before(" + beforeNowMsecs + ") = " + deltaNowMsecs + " = " + (deltaNowMsecs / 1000.) + " (seconds)",
                (deltaNowMsecs >= 1000) && (deltaNowMsecs <= 3000));

        assertTrue("monoMsecs: after(" + afterMonoMsecs + ") - before(" + beforeMonoMsecs + ") = " + deltaMonoMsecs + " = " + (deltaMonoMsecs / 1000.) + " (seconds)",
                (deltaMonoMsecs >= 1000) && (deltaMonoMsecs <= 3000));

        assertTrue("nanos: after(" + afterNanos + ") - before(" + beforeNanos + ") = " + deltaNanos + " = " + (deltaNanos / 1000000000.) + " (seconds)",
                (deltaNanos >= ONE_SECOND_IN_NANOS) && (deltaNanos <= 3 * ONE_SECOND_IN_NANOS));
    }
}