/*
 * Copyright 2018, Backblaze, Inc. All rights reserved. 
 */
package com.backblaze.b2.util;

import org.junit.BeforeClass;

import static com.backblaze.b2.util.B2DateTimeUtil.parseDateTime;

/**
 * This class is the base class for all of our unit tests.
 * It provides some useful "before" functionality and
 * it can provide some common helpers if that becomes
 * useful.
 */
public class B2BaseTest {
    /**
     * We want to be sure that the unit tests use the simulated clock.
     * If we don't ensure this, some test might inadvertently create
     * a real clock and then, when another test wants a clock simulator,
     * we'll hit an exception saying we're already using a real clock.
     * So...we pre-emptively create a simulator.
     */
    @BeforeClass
    public static void makeSureTheClockIsSimulator() {
        B2Clock.useSimulator(parseDateTime("2018-04-27 00:00:00"));
    }
}
