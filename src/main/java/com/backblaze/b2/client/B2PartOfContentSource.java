/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.util.B2InputStreamExcerpt;

import java.io.IOException;
import java.io.InputStream;

/**
 * B2PartOfContentSource implements B2ContentSource by providing an
 * excerpt of another B2ContentSource.
 */
class B2PartOfContentSource implements B2ContentSource {
    private final B2ContentSource source;
    private final long start;
    private final long length;

    B2PartOfContentSource(B2ContentSource source,
                          long start,
                          long length) {
        this.source = source;
        this.start = start;
        this.length = length;
    }

    @Override
    public long getContentLength() throws IOException {
        return length;
    }

    @Override
    public String getSha1OrNull() throws IOException {
        return null;
    }

    @Override
    public Long getSrcLastModifiedMillisOrNull() throws IOException {
        throw new IllegalStateException("why are we asking about the srcLastModifiedMillis of a PART?");
    }

    @Override
    public InputStream createInputStream() throws IOException {
        return new B2InputStreamExcerpt(source.createInputStream(), start, length);
    }

    @Override
    public String toString() {
        return "B2PartOfContentSource{" +
                "start=" + start +
                ", length=" + length +
                ", source=" + source +
                '}';
    }
}
