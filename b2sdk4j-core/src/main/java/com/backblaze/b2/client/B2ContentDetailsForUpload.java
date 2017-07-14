/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.util.B2IoUtils;
import com.backblaze.b2.util.B2Sha1;
import com.backblaze.b2.util.B2Sha1AppenderInputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

class B2ContentDetailsForUpload implements Closeable {
    private final long contentLength;
    private final String contentSha1HeaderValue;
    private final InputStream inputStream;

    B2ContentDetailsForUpload(B2ContentSource source) throws B2Exception {
        // get content length (which might be increased below for sha1-at-end)
        long contentLen;
        try {
            contentLen = source.getContentLength();
        } catch (IOException e) {
            throw new B2LocalException("read_failed", "failed to get contentLength from source: " + e, e);
        }

        // set the content_sha1 header & wrap the content stream to append the sha1 if needed.
        final String contentSha1;
        try {
            contentSha1 = source.getSha1OrNull();
        } catch (IOException e) {
            throw new B2LocalException("read_failed", "trouble getting sha1 from source: " + e, e);
        }

        // get the content stream (which might be wrapped below to add sha1-at-end)
        // be sure to do this after all the other things that can cause exceptions
        // because we need to take ownership of the inputStream.
        InputStream inputStream;
        try {
            inputStream = source.createInputStream();
        } catch (IOException e) {
            throw new B2LocalException("read_failed", "failed to create inputStream from source: " + e, e);
        }

        final String sha1HeaderValue;
        if (contentSha1 == null) {
            // we need to append the sha1 at the end.
            sha1HeaderValue = B2Headers.HEX_DIGITS_AT_END;
            inputStream = B2Sha1AppenderInputStream.create(inputStream);
            contentLen += B2Sha1.HEX_SHA1_SIZE;
        } else {
            // we have the sha1 now, so we can send it in the headers.
            sha1HeaderValue = contentSha1;
        }

        this.contentLength = contentLen;
        this.inputStream = inputStream;
        this.contentSha1HeaderValue = sha1HeaderValue;
    }

    long getContentLength() {
        return contentLength;
    }

    String getContentSha1HeaderValue() {
        return contentSha1HeaderValue;
    }

    InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void close() {
        B2IoUtils.closeQuietly(inputStream);
    }
}
