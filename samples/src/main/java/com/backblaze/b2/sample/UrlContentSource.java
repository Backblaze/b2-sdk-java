/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.sample;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.util.B2ByteRange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * UrlContentSource is a B2ContentSource that uses HttpURLConnection
 * to fetch its input from a url.  It is smart enough to use a Range
 * header when createContentSourceWithRangeOrNull() is called.
 *
 * The 'contentLen' is the length of the range that should be returned.
 * The 'start' index is the offset of the first byte within a stream.
 */
class UrlContentSource implements B2ContentSource {
    private final String urlString;
    private final long contentLen;
    private final String sha1OrNull;
    private final long start;

    private UrlContentSource(String urlString,
                             long contentLen,
                             String sha1OrNull,
                             long start) {
        this.urlString = urlString;
        this.contentLen = contentLen;
        this.sha1OrNull = sha1OrNull;
        this.start = start;
    }

    @Override
    public long getContentLength() {
        return contentLen;
    }

    @Override
    public String getSha1OrNull() {
        return sha1OrNull;
    }

    @Override
    public Long getSrcLastModifiedMillisOrNull() {
        return null;
    }

    @Override
    public InputStream createInputStream() throws IOException {
        if (contentLen == 0) {
            // use a simple empty stream.  no need to talk to the server.
            return new ByteArrayInputStream(new byte[0]);
        }

        // ok.  there are enough bytes that we need to fetch from the server
        //      and there are enough that we can make a non-empty range.
        B2ByteRange range = B2ByteRange.between(start, start+contentLen-1);
        //System.err.println("createInputStream() for " + range + " from " + urlString);

        final URL url = new URL(urlString);
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Range", range.toString());
        urlConnection.connect();

        return urlConnection.getInputStream();
    }

    @Override
    public B2ContentSource createContentSourceWithRangeOrNull(long start, long length) throws IOException {
        if (this.start != 0) {
            // we're already a range.  we shouldn't be called again.
            throw new IllegalStateException("why is something trying to make a range of a range?");
        }
        if (start < 0 || length < 0 || (start+length) > contentLen) {
            throw new IllegalArgumentException("bad range");
        }
        // note that we don't know the sha1 for a range of the content, so we don't provide it.
        return new Builder(urlString, length).setStart(start).build();
    }

    public static class Builder {
        private final String urlString;
        private final long contentLen;
        private String sha1OrNull;
        private long start;

        Builder(String urlString, long contentLen) {
            this.urlString = urlString;
            this.contentLen = contentLen;
        }

        Builder setSha1OrNull(String sha1OrNull) {
            this.sha1OrNull = sha1OrNull;
            return this;
        }

        Builder setStart(long start) {
            this.start = start;
            return this;
        }

        UrlContentSource build() {
            return new UrlContentSource(urlString, contentLen, sha1OrNull, start);
        }
    }
}
