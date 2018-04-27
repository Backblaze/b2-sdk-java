/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2InternalErrorException;
import com.backblaze.b2.client.exceptions.B2UnauthorizedException;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class B2DefaultRetryPolicyTest extends B2BaseTest {
    private static final String OP = "operation";
    private final B2RetryPolicy policy = new B2DefaultRetryPolicy();

    @Test
    public void testDoubling() {
        //noinspection ThrowableNotThrown
        final B2Exception e = new B2InternalErrorException("eek!");

        int iAttempt = 1;
        assertEquals((Integer)  1, policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));  iAttempt++;
        assertEquals((Integer)  2, policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));  iAttempt++;
        assertEquals((Integer)  4, policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));  iAttempt++;
        assertEquals((Integer)  8, policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));  iAttempt++;
        assertEquals((Integer) 16, policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));  iAttempt++;
        assertEquals((Integer) 32, policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));  iAttempt++;
        assertEquals((Integer) 64, policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));  iAttempt++;
        assertNull(policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));
    }

    @Test
    public void test_gotRetryableAfterDelay() {
        //noinspection ThrowableNotThrown
        final B2Exception e = new B2InternalErrorException("eek!"); // retryAfterSecs=null
        //noinspection ThrowableNotThrown
        final B2Exception withDelayFromServer = new B2InternalErrorException("test", 6, "wow!"); // retryAfterSecs=6

        int iAttempt = 1;
        assertEquals((Integer) 1, policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));  iAttempt++;
        assertEquals((Integer) 2, policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));  iAttempt++;

        // we use the number from the exception, if any
        assertEquals((Integer) 6, policy.gotRetryableAfterDelay(OP, iAttempt, 0, withDelayFromServer));  iAttempt++;

        // we reset to 1 after using the number from the exception.
        assertEquals((Integer) 1, policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));  iAttempt++;
        assertEquals((Integer) 2, policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));  iAttempt++;

        // we keep using the number from the server.
        assertEquals((Integer) 6, policy.gotRetryableAfterDelay(OP, iAttempt, 0, withDelayFromServer));  iAttempt++;
        assertEquals((Integer) 6, policy.gotRetryableAfterDelay(OP, iAttempt, 0, withDelayFromServer));  iAttempt++;
        assertNull(policy.gotRetryableAfterDelay(OP, iAttempt, 0, e));
    }

    @Test
    public void test_gotRetryableImmediately() {
        final B2Exception e = B2UnauthorizedException.create("test", 401, null, "msg");

        // verify that we stop after some number of attempts.

        int iAttempt = 1;
        assertTrue(policy.gotRetryableImmediately(OP, iAttempt, 0, e));  iAttempt++;
        assertTrue(policy.gotRetryableImmediately(OP, iAttempt, 0, e));  iAttempt++;
        assertTrue(policy.gotRetryableImmediately(OP, iAttempt, 0, e));  iAttempt++;
        assertTrue(policy.gotRetryableImmediately(OP, iAttempt, 0, e));  iAttempt++;
        assertTrue(policy.gotRetryableImmediately(OP, iAttempt, 0, e));  iAttempt++;
        assertTrue(policy.gotRetryableImmediately(OP, iAttempt, 0, e));  iAttempt++;
        assertTrue(policy.gotRetryableImmediately(OP, iAttempt, 0, e));  iAttempt++;
        assertTrue(!policy.gotRetryableImmediately(OP, iAttempt, 0, e));
    }
}
