/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class B2CancellableContentSourceTest {

    final InputStream mockInputStream = mock(InputStream.class);
    final B2ContentSource mockContentSource = mock(B2ContentSource.class);
    final B2CancellationToken cancellationToken = new B2CancellationToken();

    final B2CancellableContentSource cancellableContentSource = new B2CancellableContentSource(mockContentSource, cancellationToken);

    @Before
    public void setup() throws B2Exception, IOException {
        when(mockContentSource.createInputStream()).thenReturn(mockInputStream);
    }
    @Test
    public void testPassthroughMethods() throws B2Exception, IOException {
        cancellableContentSource.getContentLength();
        verify(mockContentSource).getContentLength();

        cancellableContentSource.getSha1OrNull();
        verify(mockContentSource).getSha1OrNull();

        cancellableContentSource.getSrcLastModifiedMillisOrNull();
        verify(mockContentSource).getSrcLastModifiedMillisOrNull();
    }

    @Test
    public void testInputStreamPassthrough() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();

        inputStream.read();
        verify(mockInputStream).read();

        final byte[] bytes = new byte[10];
        inputStream.read(bytes);
        verify(mockInputStream).read(eq(bytes));

        inputStream.read(bytes, 0, 10);
        verify(mockInputStream).read(eq(bytes), eq(0), eq(10));

        inputStream.skip(1);
        verify(mockInputStream).skip(1);

        inputStream.close();
        verify(mockInputStream).close();

        inputStream.mark(2);
        verify(mockInputStream).mark(2);

        inputStream.reset();
        verify(mockInputStream).reset();

        inputStream.markSupported();
        verify(mockInputStream).markSupported();
    }

    @Test(expected = IOException.class)
    public void testInputStreamReadCancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        inputStream.read();
    }

    @Test(expected = IOException.class)
    public void testInputStreamRead2Cancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        inputStream.read(new byte[1]);
    }

    @Test(expected = IOException.class)
    public void testInputStreamRead3Cancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        inputStream.read(new byte[10], 0, 10);
    }

    @Test(expected = IOException.class)
    public void testInputStreamSkipCancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        inputStream.skip(1);
    }

    @Test(expected = IOException.class)
    public void testInputStreamAvailableCancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        inputStream.available();
    }

    @Test(expected = IOException.class)
    public void testInputStreamCloseCancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        inputStream.close();
    }

    @Test(expected = IOException.class)
    public void testInputStreamResetCancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        inputStream.reset();
    }
}


