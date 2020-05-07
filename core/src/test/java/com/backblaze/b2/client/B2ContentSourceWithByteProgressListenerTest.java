/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.util.B2BaseTest;
import com.backblaze.b2.util.B2ByteProgressListener;
import com.backblaze.b2.util.B2InputStreamWithByteProgressListener;
import org.junit.After;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class B2ContentSourceWithByteProgressListenerTest extends B2BaseTest {
    private static final int EOF = -1;

    private final B2ContentSource wrappedSource = mock(B2ContentSource.class);
    private final B2ByteProgressListener listener = mock(B2ByteProgressListener.class);
    private final B2ContentSourceWithByteProgressListener source = new B2ContentSourceWithByteProgressListener(wrappedSource, listener);

    @Test
    public void testGetContentLength() throws Exception {
        when(wrappedSource.getContentLength()).thenReturn(6L);
        assertEquals(6L, source.getContentLength());
    }

    @Test
    public void testGetSha1OrNull() throws Exception {
        when(wrappedSource.getSha1OrNull()).thenReturn("sha1");
        assertEquals("sha1", source.getSha1OrNull());
    }

    @Test
    public void testGetSrcLastModifiedMillisOrNull() throws Exception {
        when(wrappedSource.getSrcLastModifiedMillisOrNull()).thenReturn(6L);
        assertEquals((Long) 6L, source.getSrcLastModifiedMillisOrNull());
    }

    @Test
    public void testCreateInputStream() throws Exception {
        final InputStream wrappedStream = mock(InputStream.class);
        when(wrappedSource.createInputStream()).thenReturn(wrappedStream);
        when(wrappedStream.read()).thenReturn((int) 'a', (int) 'b', EOF);

        final InputStream stream = source.createInputStream();
        assertTrue(stream instanceof B2InputStreamWithByteProgressListener);

        assertEquals('a', stream.read());
        verify(listener, times(1)).progress(1);

        assertEquals('b', stream.read());
        verify(listener, times(1)).progress(2);

        assertEquals(EOF, stream.read());
        verify(listener, times(1)).reachedEof(2);
    }


    @Test
    public void testToString() throws Exception {
        assertTrue(source.toString().startsWith("B2ContentSourceWithByteProgressListener{Mock for B2ContentSource"));
    }

    @After
    public void cleanup() {
        verifyNoMoreInteractions(listener);
    }

}