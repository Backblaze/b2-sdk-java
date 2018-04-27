package com.backblaze.b2.util;/*
 * Copyright 2017, Backblaze, Inc. All rights reserved. 
 */

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class B2InputStreamWithByteProgressListenerTest extends B2BaseTest {
    private static final int EOF = -1;

    private final InputStream wrappedStream = mock(InputStream.class);
    private final B2ByteProgressListener listener = mock(B2ByteProgressListener.class);
    private final InputStream stream = new B2InputStreamWithByteProgressListener(wrappedStream, listener);

    private final byte[] V = new byte[100];

    @After
    public void cleanup() {
        verifyNoMoreInteractions(wrappedStream);
        verifyNoMoreInteractions(listener);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // test read().
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void readByte_getByte() throws Exception {
        when(wrappedStream.read()).thenReturn((int) 'a');
        assertEquals('a', stream.read());

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).read();
        verify(listener, times(1)).progress(1);
    }

    @Test
    public void readByte_getEof() throws Exception {
        when(wrappedStream.read()).thenReturn(EOF);
        assertEquals(EOF, stream.read());

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).read();
        verify(listener, times(1)).reachedEof(0);
    }

    @Test
    public void readByte_throwsIOException() throws Exception {
        final IOException exception = new IOException("test");
        when(wrappedStream.read()).thenThrow(exception);

        try {
            //noinspection ResultOfMethodCallIgnored
            stream.read();
            fail("should've thrown!");
        } catch (IOException e) {
            assertEquals("test", e.getMessage());
        }

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).read();
        verify(listener, times(1)).hitException(eq(exception), eq(0L));
    }

    @Test
    public void readByte_throwsRuntimeException() throws Exception {
        final RuntimeException exception = new RuntimeException("test");
        when(wrappedStream.read()).thenThrow(exception);

        try {
            //noinspection ResultOfMethodCallIgnored
            stream.read();
            fail("should've thrown!");
        } catch (RuntimeException e) {
            assertEquals("test", e.getMessage());
        }

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).read();
        verify(listener, times(1)).hitException(eq(exception), eq(0L));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // test read(byte[]) & test read(byte[], offset, len)
    //    i know the first is implemented with the second.
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void testReadArray() throws Exception {
        when(wrappedStream.read(V, 0, V.length)).thenReturn(6);

        assertEquals(6, stream.read(V));

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).read(V, 0, V.length);
        verify(listener, times(1)).progress(6);
    }

    @Test
    public void testReadArray_getEof() throws Exception {
        when(wrappedStream.read(V, 0, V.length)).thenReturn(EOF);

        assertEquals(EOF, stream.read(V));

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).read(V, 0, V.length);
        verify(listener, times(1)).reachedEof(0);
    }

    @Test
    public void testReadArrayOffsetAndLength_throwsIOException() throws Exception {
        final IOException exception = new IOException("test");
        when(wrappedStream.read(V, 1, V.length-10)).thenThrow(exception);

        try {
            //noinspection ResultOfMethodCallIgnored
            stream.read(V, 1, V.length-10);
            fail("should've thrown!");
        } catch (IOException e) {
            assertEquals("test", e.getMessage());
        }

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).read(V, 1, V.length-10);
        verify(listener, times(1)).hitException(exception, 0);
    }

    @Test
    public void testReadArrayOffsetAndLength_throwsRuntimeException() throws Exception {
        final RuntimeException exception = new RuntimeException("test");
        when(wrappedStream.read(V, 1, V.length-10)).thenThrow(exception);

        try {
            //noinspection ResultOfMethodCallIgnored
            stream.read(V, 1, V.length-10);
            fail("should've thrown!");
        } catch (RuntimeException e) {
            assertEquals("test", e.getMessage());
        }

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).read(V, 1, V.length-10);
        verify(listener, times(1)).hitException(exception, 0);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // test skip().
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void testSkip() throws Exception {
        when(wrappedStream.skip(6)).thenReturn(3L);
        assertEquals(3L, stream.skip(6));

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).skip(6);
        verify(listener, times(1)).progress(3);
    }

    @Test
    public void testSkip_getEof() throws Exception {
        when(wrappedStream.skip(anyLong())).thenReturn((long) EOF);
        assertEquals(EOF, stream.skip(6));

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).skip(6);
        verify(listener, times(1)).reachedEof(0);
    }

    @Test
    public void testSkip_throwsIOException() throws Exception {
        final IOException exception = new IOException("test");
        when(wrappedStream.skip(anyLong())).thenThrow(exception);

        try {
            //noinspection ResultOfMethodCallIgnored
            stream.skip(6);
            fail("should've thrown!");
        } catch (IOException e) {
            assertEquals("test", e.getMessage());
        }

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).skip(6);
        verify(listener, times(1)).hitException(eq(exception), eq(0L));
    }

    @Test
    public void testSkip_throwsRuntimeException() throws Exception {
        final RuntimeException exception = new RuntimeException("test");
        when(wrappedStream.skip(anyLong())).thenThrow(exception);

        try {
            //noinspection ResultOfMethodCallIgnored
            stream.skip(6);
            fail("should've thrown!");
        } catch (RuntimeException e) {
            assertEquals("test", e.getMessage());
        }

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).skip(6);
        verify(listener, times(1)).hitException(eq(exception), eq(0L));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // test available().
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void testAvailable() throws Exception {
        when(wrappedStream.available()).thenReturn(6);

        assertEquals(6, stream.available());

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).available();
    }

    @Test
    public void testAvailable_throwsIOException() throws Exception {
        final IOException exception = new IOException("test");
        when(wrappedStream.available()).thenThrow(exception);

        try {
            //noinspection ResultOfMethodCallIgnored
            stream.available();
            fail("should've thrown!");
        } catch (IOException e) {
            assertEquals("test", e.getMessage());
        }

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).available();
        verify(listener, times(1)).hitException(eq(exception), eq(0L));
    }

    @Test
    public void testAvailable_throwsRuntimeException() throws Exception {
        final RuntimeException exception = new RuntimeException("test");
        when(wrappedStream.available()).thenThrow(exception);

        try {
            //noinspection ResultOfMethodCallIgnored
            stream.available();
            fail("should've thrown!");
        } catch (RuntimeException e) {
            assertEquals("test", e.getMessage());
        }

        //noinspection ResultOfMethodCallIgnored
        verify(wrappedStream, times(1)).available();
        verify(listener, times(1)).hitException(eq(exception), eq(0L));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // test close().
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void close() throws Exception {
        stream.close();

        verify(wrappedStream, times(1)).close();
    }

}