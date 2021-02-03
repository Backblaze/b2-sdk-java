/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for B2CancellationToken
 */
public class B2CancellationTokenTest {
    private final B2CancellationToken cancellationToken = new B2CancellationToken();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testThrowIfCancelledDoesThrow() throws B2Exception {
        cancellationToken.cancel();

        thrown.expect(B2LocalException.class);
        cancellationToken.throwIfCancelled();
    }

    @Test
    public void testThrowIfCancelledDoesNotThrow() throws B2Exception {
        cancellationToken.throwIfCancelled();
    }

    @Test
    public void testCancelWillThrowIfCalledTwice() throws B2Exception {
        cancellationToken.cancel();

        thrown.expect(B2LocalException.class);
        cancellationToken.cancel();
    }

    @Test
    public void testIsCancelled() throws B2Exception {
        Assert.assertFalse(cancellationToken.isCancelled());

        cancellationToken.cancel();

        Assert.assertTrue(cancellationToken.isCancelled());
    }
}
