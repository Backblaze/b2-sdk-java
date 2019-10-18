/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.util.B2ByteProgressListener;
import com.backblaze.b2.util.B2InputStreamWithByteProgressListener;
import com.backblaze.b2.util.B2Preconditions;

import java.io.IOException;
import java.io.InputStream;

/**
 * B2ContentSourceWithByteProgressListener implements B2ContentSource by wrapping
 * another content source and arranging to report on the progress through its
 * input streams.  Note that if createInputStream is called multiple times,
 * you may get progress from any and all of the streams through the listener.
 * (In intended usage, there's only one input stream at a time and that's fine!)
 */
class B2ContentSourceWithByteProgressListener implements B2ContentSource {
    private final B2ContentSource source;
    private final B2ByteProgressListener listener;

    B2ContentSourceWithByteProgressListener(B2ContentSource source,
                                            B2ByteProgressListener listener) {
        B2Preconditions.checkArgumentIsNotNull(listener, "listener");
        this.source = source;
        this.listener = listener;
    }

    @Override
    public long getContentLength() throws IOException {
        return source.getContentLength();
    }

    @Override
    public String getSha1OrNull() throws IOException {
        return source.getSha1OrNull();
    }

    @Override
    public Long getSrcLastModifiedMillisOrNull() throws IOException {
        return source.getSrcLastModifiedMillisOrNull();
    }

    @Override
    public InputStream createInputStream() throws IOException, B2Exception {
        final InputStream inputStream = source.createInputStream();
        return new B2InputStreamWithByteProgressListener(inputStream, listener);
    }

    @Override
    public String toString() {
        return "B2ContentSourceWithByteProgressListener{" + source + "}";
    }
}
