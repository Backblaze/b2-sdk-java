/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class B2ClockImplTest extends B2BaseTest {

    @Test
    public void test() throws InterruptedException {
        final B2ClockImpl clock = new B2ClockImpl();

        final long beforeNowMillis = clock.wallClockMillis();
        final long beforeMonoMillis = clock.monotonicMillis();

        Thread.sleep(1000);

        final long afterNowMillis = clock.wallClockMillis();
        final long afterMonoMillis = clock.monotonicMillis();

        final long deltaNowMillis = afterNowMillis - beforeNowMillis;
        final long deltaMonoMillis = afterMonoMillis - beforeMonoMillis;

        assertTrue("nowMillis: after(" + afterNowMillis + ") - before(" + beforeNowMillis + ") = " + deltaNowMillis + " = " + (deltaNowMillis / 1000.) + " (seconds)",
                (deltaNowMillis >= 1000) && (deltaNowMillis <= 3000));

        assertTrue("monoMillis: after(" + afterMonoMillis + ") - before(" + beforeMonoMillis + ") = " + deltaMonoMillis + " = " + (deltaMonoMillis / 1000.) + " (seconds)",
                (deltaMonoMillis >= 1000) && (deltaMonoMillis <= 3000));
    }
}