/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.backblaze.b2.util.B2DateTimeUtil.getMillisecondsSinceEpoch;

/**
 * B2ClockSim provides a simple
 */
public class B2ClockSim extends B2Clock {

    private long nowMillis;
    private long nanos;

    B2ClockSim(LocalDateTime startTime) {
        resetBoth(startTime);
    }

    @Override
    public long getMonoNanoTime() {
        return nanos;
    }

    @Override
    public long getNowMsecTime() {
        return nowMillis;
    }

    /**
     * Shifts the current time by the given duration.
     *    the nowMillisTime and monoNanoTime will go forward by the same amount (mod resolution!)
     * @param delta the amount of time to shift both clocks by.
     *              must be non-negative to avoid making the monotonic clock go backwards!
     */
    public void advanceBoth(Duration delta) {
        B2Preconditions.checkArgument(delta.toNanos() >= 0, "delta must be non-negative");
        nowMillis += delta.toMillis();
        nanos += delta.toNanos();
    }

    /**
     * Shifts the current wall clock time by the given duration.
     * Only nowMills will be adjusted.  Use this when you want to
     * shift time backwards, since you're not allowed to call advanceBoth()
     * with negative value.
     * @param delta the time to advance the wall clock by
     */
    public void advanceNow(Duration delta) {
        nowMillis += delta.toMillis();
    }

    /**
     * This is intended to be run between tests to reset the clock to an initial
     * state, as if the simulator has just be constructed.
     *
     * @param desiredNow the desired wall clock time.
     */
    public void resetBoth(LocalDateTime desiredNow) {
        this.nowMillis = getMillisecondsSinceEpoch(desiredNow);
        this.nanos = 0;
    }
}
