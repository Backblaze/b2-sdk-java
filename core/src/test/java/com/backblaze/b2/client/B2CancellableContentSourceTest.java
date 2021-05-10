/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class B2CancellableContentSourceTest {

    final InputStream mockInputStream = mock(InputStream.class);
    final B2ContentSource mockContentSource = mock(B2ContentSource.class);
    final B2CancellationToken cancellationToken = new B2CancellationToken();

    final B2CancellableContentSource cancellableContentSource = new B2CancellableContentSource(mockContentSource, cancellationToken);

    final long EXPECTED_CONTENT_LENGTH = 100;
    final String EXPECTED_SHA1 = "0000000000000000000000000000000000000000";
    final Long EXPECTED_SRC_MODIFIED = 123L;
    final int EXPECTED_READ_RESULT = 1;
    final long EXPECTED_SKIP_RESULT = 2;
    final boolean EXPECTED_MARK_SUPPORTED_RESULT = true;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() throws B2Exception, IOException {
        when(mockContentSource.createInputStream()).thenReturn(mockInputStream);

        // set return values
        when(mockContentSource.getContentLength()).thenReturn(EXPECTED_CONTENT_LENGTH);
        when(mockContentSource.getSha1OrNull()).thenReturn(EXPECTED_SHA1);
        when(mockContentSource.getSrcLastModifiedMillisOrNull()).thenReturn(EXPECTED_SRC_MODIFIED);

        when(mockInputStream.read()).thenReturn(EXPECTED_READ_RESULT);
        when(mockInputStream.read(anyObject())).thenReturn(EXPECTED_READ_RESULT);
        when(mockInputStream.read(anyObject(), anyInt(), anyInt())).thenReturn(EXPECTED_READ_RESULT);

        when(mockInputStream.skip(anyLong())).thenReturn(EXPECTED_SKIP_RESULT);
        when(mockInputStream.markSupported()).thenReturn(EXPECTED_MARK_SUPPORTED_RESULT);
    }

    @Test
    public void testPassthroughMethods() throws IOException {
        Assert.assertEquals(EXPECTED_CONTENT_LENGTH, cancellableContentSource.getContentLength());
        verify(mockContentSource).getContentLength();

        Assert.assertEquals(EXPECTED_SHA1, cancellableContentSource.getSha1OrNull());
        verify(mockContentSource).getSha1OrNull();

        Assert.assertEquals(EXPECTED_SRC_MODIFIED, cancellableContentSource.getSrcLastModifiedMillisOrNull());
        verify(mockContentSource).getSrcLastModifiedMillisOrNull();
    }

    @Test
    public void testInputStreamPassthrough() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();

        Assert.assertEquals(EXPECTED_READ_RESULT, inputStream.read());
        verify(mockInputStream).read();

        final byte[] bytes = new byte[10];
        Assert.assertEquals(EXPECTED_READ_RESULT, inputStream.read(bytes));
        verify(mockInputStream).read(eq(bytes));

        Assert.assertEquals(EXPECTED_READ_RESULT, inputStream.read(bytes, 0, 10));
        verify(mockInputStream).read(eq(bytes), eq(0), eq(10));

        Assert.assertEquals(EXPECTED_SKIP_RESULT, inputStream.skip(1));
        verify(mockInputStream).skip(1);

        inputStream.close();
        verify(mockInputStream).close();

        inputStream.mark(2);
        verify(mockInputStream).mark(2);

        inputStream.reset();
        verify(mockInputStream).reset();

        Assert.assertEquals(EXPECTED_MARK_SUPPORTED_RESULT, inputStream.markSupported());
        verify(mockInputStream).markSupported();
    }

    @Test
    public void testInputStreamReadCancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        thrown.expect(IOException.class);
        inputStream.read();
    }

    @Test
    public void testInputStreamRead2Cancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        thrown.expect(IOException.class);
        inputStream.read(new byte[1]);
    }

    @Test
    public void testInputStreamRead3Cancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        thrown.expect(IOException.class);
        inputStream.read(new byte[10], 0, 10);
    }

    @Test
    public void testInputStreamSkipCancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        thrown.expect(IOException.class);
        inputStream.skip(1);
    }

    @Test
    public void testInputStreamAvailableCancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        thrown.expect(IOException.class);
        inputStream.available();
    }

    @Test
    public void testInputStreamCloseCancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        thrown.expect(IOException.class);
        inputStream.close();
    }

    @Test
    public void testInputStreamResetCancelled() throws B2Exception, IOException {
        final InputStream inputStream = cancellableContentSource.createInputStream();
        cancellationToken.cancel();

        thrown.expect(IOException.class);
        inputStream.reset();
    }
}


