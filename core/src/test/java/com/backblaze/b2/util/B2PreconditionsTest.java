/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class B2PreconditionsTest {
    @Test
    public void test() {
        checkThrows(IllegalArgumentException.class, null, () -> B2Preconditions.checkArgument(false));
        checkThrows(IllegalArgumentException.class, "test", () -> B2Preconditions.checkArgument(false, "test"));
        checkThrows(IllegalStateException.class, null, () -> B2Preconditions.checkState(false));
        checkThrows(IllegalStateException.class, "test", () -> B2Preconditions.checkState(false, "test"));

        checkDoesntThrow(() -> B2Preconditions.checkArgument(true));
        checkDoesntThrow(() -> B2Preconditions.checkArgument(true, "test"));
        checkDoesntThrow(() -> B2Preconditions.checkState(true));
        checkDoesntThrow(() -> B2Preconditions.checkState(true, "test"));
    }

    private void checkDoesntThrow(Runnable toRun) {
        toRun.run();
    }

    private void checkThrows(Class<?> expectedClass,
                             String expectedMsg,
                             Runnable toRun) {
        boolean threw = false;
        try {
            toRun.run();
        } catch (Throwable t) {
            threw = true;
            assertEquals(expectedClass, t.getClass());
            assertEquals(expectedMsg, t.getMessage());
        }
        assertTrue(threw);
    }
}
