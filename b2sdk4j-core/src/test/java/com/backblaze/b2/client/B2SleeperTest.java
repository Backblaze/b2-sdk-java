/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

public class B2SleeperTest {
    private final B2Sleeper sleeper = new B2Sleeper();

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testSleeping() throws B2Exception {
        final long beforeMsec = System.currentTimeMillis();
        sleeper.sleepSecondsOrThrow(1, new B2LocalException("test", "testing"));
        final long afterMsec = System.currentTimeMillis();

        // we should've slept most of a second! (and not thrown)
        assertTrue((afterMsec - beforeMsec) > 900);
    }

    @Test
    public void testInterruptedSleep() throws B2Exception {
        // interrupt this thread, so sleeping will throw.
        Thread.currentThread().interrupt();

        thrown.expect(B2Exception.class);
        thrown.expectMessage("testing");

        try {
            // try to sleep.  it should throw.
            sleeper.sleepSecondsOrThrow(1, new B2LocalException("test", "testing"));
        } finally {
            assertTrue(Thread.interrupted()); // this clears the flag.
            // if we don't clear the flag, IDEA's code coverage doesn't think we ran code.
        }
    }
}
