/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

/**
 * B2ClockImpl uses values from the System class to provide a "real"
 * implementation of B2Clock whose clocks are determined by the system.
 */
public class B2ClockImpl extends B2Clock {
    /**
     * 'monoNanoBase is the first nanoTime we get from the system.
     * We use it as a baseline to avoid wrapping the monoNanoTime
     * we return.
     */
    private final long monoNanoBase;

    public B2ClockImpl() {
         monoNanoBase = System.nanoTime();
    }

    @Override
    public long getMonoNanoTime() {
        // subtraction gets the right value even if nanoTime() has wrapped past Long.MAX_VALUE.
        return System.nanoTime() - monoNanoBase;
    }

    @Override
    public long getNowMsecTime() {
        return System.currentTimeMillis();
    }
}
