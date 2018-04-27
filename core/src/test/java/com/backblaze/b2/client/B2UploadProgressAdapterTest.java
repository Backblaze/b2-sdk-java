package com.backblaze.b2.client;/*
 * Copyright 2017, Backblaze, Inc. All rights reserved. 
 */

import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadProgress;
import com.backblaze.b2.util.B2BaseTest;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class B2UploadProgressAdapterTest extends B2BaseTest {
    private final B2UploadListener listener = mock(B2UploadListener.class);
    private final B2UploadProgressAdapter adapter = new B2UploadProgressAdapter(listener, 1, 2, 3, 4);

    @After
    public void cleanup() {
        verify(listener, times(1)).progress(anyObject());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void progress() throws Exception {
        adapter.progress(5);
        assertEquals("B2UploadProgress{partIndex=1, partCount=2, startByte=3, length=4, bytesSoFar=5, state=UPLOADING}",
                getProgressString());
    }

    @Test
    public void hitException() throws Exception {
        final RuntimeException e = new RuntimeException("testing");
        adapter.hitException(e, 6);
        assertEquals("B2UploadProgress{partIndex=1, partCount=2, startByte=3, length=4, bytesSoFar=6, state=FAILED}",
                getProgressString());
    }

    @Test
    public void reachedEof() throws Exception {
        adapter.reachedEof(7);
        assertEquals("B2UploadProgress{partIndex=1, partCount=2, startByte=3, length=4, bytesSoFar=7, state=UPLOADING}",
                getProgressString());
    }

    private String getProgressString() {
        ArgumentCaptor<B2UploadProgress> progressCaptor = ArgumentCaptor.forClass(B2UploadProgress.class);
        verify(listener).progress(progressCaptor.capture());
        return progressCaptor.getValue().toString();
    }

}