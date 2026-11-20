/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class B2CancellableContentSourceFindingsTest extends B2BaseTest {
    /** Finding C35: closing the stream after a cancel may complain, but the wrapped stream must still be closed. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC35_wrappedStreamClosedEvenAfterCancel() throws Exception {
        final boolean[] closed = {false};
        final InputStream tracked = new ByteArrayInputStream(new byte[10]) {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };
        final B2ContentSource wrapped = mock(B2ContentSource.class);
        when(wrapped.createInputStream()).thenReturn(tracked);
        final B2CancellationToken token = new B2CancellationToken();
        final InputStream in = new B2CancellableContentSource(wrapped, token).createInputStream();

        token.cancel();
        try {
            in.close();
        } catch (IOException ignoredForThisFinding) {
            // whether close() reports the cancellation is not what this finding is about.
        }
        assertTrue("the wrapped stream was left open", closed[0]);
    }
}
