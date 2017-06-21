/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentHandlers;

import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is a B2ContentWriter which records the incoming data into an
 * in-memory byte array.
 *
 * This will completely fail if the contentLen is more than Integer.MAX_VALUE bytes.
 * It may cause other memory-related issues before that.
 */
public class B2ContentMemoryWriter extends B2ContentWriter {
    private static final long MAX_LEN = Integer.MAX_VALUE;
    private ByteArrayOutputStream byteStream = null; // most recent byteStream created.  null at least until readContent is called.

    private B2ContentMemoryWriter(boolean verifySha1ByRereadingFromDestination) {
        super(verifySha1ByRereadingFromDestination);
    }

    @Override
    public void readContent(B2Headers responseHeaders,
                            InputStream rawIn) throws B2Exception {
        final long len = responseHeaders.getContentLength();
        if (len >= MAX_LEN) {
            throw new B2LocalException("too_big",
                    "contentLength is too big for this B2ContentMemoryWriter.  (" + len + " > " + MAX_LEN + ")");
        }
        super.readContent(responseHeaders, rawIn);
    }

    @Override
    protected OutputStream createDestinationOutputStream() throws IOException {
        byteStream = new ByteArrayOutputStream();
        return byteStream;
    }

    @Override
    protected InputStream createDestinationInputStream() throws IOException {
        return new ByteArrayInputStream(getBytes());
    }

    /**
     * @apiNote this might be empty or not quite what you expect if readContents()
     *          hasn't completed successfully.
     * @return null if readContent hasn't made it far enough
     *         or a new byteArray with the contents that were read.
     */
    public byte[] getBytes() {
        if (byteStream == null) {
            return new byte[0];
        } else {
            return byteStream.toByteArray();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static B2ContentMemoryWriter build() {
        return builder().build();
    }

    public static class Builder {
        private boolean verifySha1ByRereadingFromDestination = true;

        public B2ContentMemoryWriter build() {
            return new B2ContentMemoryWriter(verifySha1ByRereadingFromDestination);
        }

        @SuppressWarnings("SameParameterValue")
        public Builder setVerifySha1ByRereadingFromDestination(boolean verifySha1ByRereadingFromDestination) {
            this.verifySha1ByRereadingFromDestination = verifySha1ByRereadingFromDestination;
            return this;
        }
    }

}
