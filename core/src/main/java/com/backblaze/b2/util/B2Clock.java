/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.time.LocalDateTime;

/**
 * This class provides access to two different time measurements.
 *
 * It also provides access to the global clock object.  It's an object
 * instead of static methods so we can use a different implementation
 * in tests.
 *
 * It is an abstract class instead of an interface so that it can hold the
 * global instance. (If we had a dependency injection system we would
 * probably use that instead of a global, but we don't have one.)
 *
 * I also keep thinking that maybe there should be two different types
 * of clock objects -- one for monotonic time and one for wall clock time.
 * So far, I'm keeping them together because it helps to advance them
 * both in tests and every call to get a value says what type of time it is.
 */
public abstract class B2Clock {
    // only access with while synchronized(B2Clock.class).
    private static B2Clock theClock;

    /**
     * If theClock is null, this will create a B2ClockSim with the desiredNow.
     * If theClock is a B2ClockSim, this will set it to the specified time.
     * If theclock is not a B2ClockSim, this will throw.
     * @param desiredNow is the wall clock time for the simulator's "now".
     * @return theClock
     */
    public static B2ClockSim useSimulator(LocalDateTime desiredNow) {
        if (theClock == null) {
            theClock = new B2ClockSim(desiredNow);
        }
        B2Preconditions.checkState(theClock instanceof B2ClockSim,
                "theClock must be a B2ClockSim, but it's a " + theClock.getClass().getSimpleName());

        final B2ClockSim sim = (B2ClockSim) theClock;
        sim.resetBoth(desiredNow);
        return sim;
    }

    /**
     * @return theClock to use.
     */
    public static synchronized B2Clock get() {
        if (theClock == null) {
            theClock = new B2ClockImpl();
        }
        return theClock;
    }

    /**
     * @return a monotonically increasing number of nanoseconds.  note that it has
     *         no relationship to wall clock time and will differ between different
     *         runs of the JVM.  note that it won't necessarily be as precise as
     *         nanoseconds.  See System.nanoTime().
     *
     *         It is monotonically increasing!
     *         AND it won't wrap during any single run of a JVM.
     *         (unless the JVM runs for hundreds of of years!
     *         (2^63 nanos) / (10^9 nanos/sec) / (86400 secs/day) / (365 days/year) ~= 292 years)
     */
    public abstract long getMonoNanoTime();

    /**
     * @return This is just a milliseconds version of getMonoNanoTime().
     */
    public long getMonoMsecTime() {
        return getMonoNanoTime() / B2DateTimeUtil.ONE_MSEC_IN_NANOS;
    }

    /**
     * @return the number of milliseconds since the unix epoch.
     *         may be negative if now is before the epoch.
     *         the values returned by this represent wall clock
     *         time and might not be monotonic if the system clock
     *         is changed.  time might sometimes flow at a variable
     *         speed or even backwards if the clock is being adjusted.
     */
    public abstract long getNowMsecTime();
}
