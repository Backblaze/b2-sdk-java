/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

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
     * This can be used in tests to set theClock to a mock or simulator clock.
     * It must not be called after get() has been called.
     * @param clock the clock to use.
     */
    public static synchronized void set(B2Clock clock) {
        B2Preconditions.checkState(theClock == null, "can't change clocks!");
        B2Preconditions.checkArgumentIsNotNull(clock, "clock");
        theClock = clock;
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
     *         (unless the JVM runs for hundreds of thousands of millenia!
     *         (2^63 nanos) / (2^9 nanos/sec) / 86400 (secs/day) / 365 (days/year) / 1000 (years/millenia) ~= 571,232 millenia)
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
