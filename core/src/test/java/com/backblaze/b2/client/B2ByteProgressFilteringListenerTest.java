package com.backblaze.b2.client;/*
 * Copyright 2017, Backblaze, Inc. All rights reserved. 
 */

import com.backblaze.b2.util.B2ByteProgressListener;
import com.backblaze.b2.util.B2Clock;
import com.backblaze.b2.util.B2ClockSim;
import org.junit.After;
import org.junit.Test;

import java.time.Duration;

import static com.backblaze.b2.util.B2DateTimeUtil.parseDateTime;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class B2ByteProgressFilteringListenerTest {
    private final B2ClockSim clockSim = B2Clock.useSimulator(parseDateTime("2017-08-31 00:00:00"));
    private final B2ByteProgressListener wrapped = mock(B2ByteProgressListener.class);
    private final B2ByteProgressListener filtering = new B2ByteProgressFilteringListener(wrapped);
    private final Exception exception = new RuntimeException("intentional");

    @After
    public void shutdown() {
        verifyNoMoreInteractions(wrapped);
    }

    @Test
    public void test() {
        long bytesSoFar = 100;


        checkReachedEofAndException(bytesSoFar);
        checkProgressCalled(bytesSoFar); // the first progress should be called

        // let a little time and bytes pass.  too soon for progress()
        {
            bytesSoFar += 1;
            clockSim.advanceBoth(Duration.ofMillis(4900));
            assertEquals(4900, clockSim.getMonoMsecTime());

            checkReachedEofAndException(bytesSoFar);
            checkProgressNotCalled(bytesSoFar); // no change. too soon.
        }

        // let a little more time and bytes pass.  still too soon for progress()
        {
            bytesSoFar += 1;
            clockSim.advanceBoth(Duration.ofMillis(99));
            assertEquals(4999, clockSim.getMonoMsecTime());

            checkReachedEofAndException(bytesSoFar);
            checkProgressNotCalled(bytesSoFar); // no change. too soon.
        }

        // check that having no time passing doesn't let events through.
        {
            bytesSoFar += 1;
            checkProgressNotCalled(bytesSoFar); // no change. too soon.
        }

        // let time and bytes pass.  just barely enough to be long enough for next call.
        {
            bytesSoFar += 1;
            clockSim.advanceBoth(Duration.ofMillis(1));
            assertEquals(5000, clockSim.getMonoMsecTime());

            checkReachedEofAndException(bytesSoFar);
            checkProgressCalled(bytesSoFar);
        }

        // again, not enough to let progress through
        {
            bytesSoFar += 1;
            clockSim.advanceBoth(Duration.ofMillis(999));
            assertEquals(5999, clockSim.getMonoMsecTime());

            checkReachedEofAndException(bytesSoFar);
            checkProgressNotCalled(bytesSoFar);
        }

        // again, enough to let progress through & not exactly at threshold.
        {
            bytesSoFar += 1;
            clockSim.advanceBoth(Duration.ofMillis(4002));
            assertEquals(10001, clockSim.getMonoMsecTime());

            checkReachedEofAndException(bytesSoFar);
            checkProgressCalled(bytesSoFar);
        }
    }

    // reachedEof & hitException are always passed along.
    private void checkReachedEofAndException(long bytesSoFar) {
        filtering.reachedEof(bytesSoFar);
        verify(wrapped, times(1)).reachedEof(bytesSoFar);

        filtering.hitException(exception, bytesSoFar);
        verify(wrapped, times(1)).hitException(exception, bytesSoFar);
    }

    private void checkProgressCalled(long bytesSoFar) {
        filtering.progress(bytesSoFar);
        verify(wrapped, times(1)).progress(bytesSoFar);
    }

    private void checkProgressNotCalled(long bytesSoFar) {
        filtering.progress(bytesSoFar);
        verify(wrapped, never()).progress(bytesSoFar);
    }
}