/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentSources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Use B2ByteArrayContentSource to wrap data in memory for uploading to B2.
 * Don't change the data while it's being uploaded.
 *
 * If you know the sha1, you're encouraged to provide it.  See B2ContentSource.
 *
 */
public class B2ByteArrayContentSource implements B2ContentSource {
    private final byte[] source;
    private final String sha1OrNull;
    private final Long srcLastModifiedMillisOrNull;


    /**
     * The caller must not change the bytes after calling this.
     * @param source the bytes to send
     * @param sha1OrNull the sha1 to return.
     * @param srcLastModifiedMillisOrNull the srcLastModifiedMillisOrNull to return.
     */
    private B2ByteArrayContentSource(byte[] source,
                                     String sha1OrNull,
                                     Long srcLastModifiedMillisOrNull) {
        this.source = source;
        this.sha1OrNull = sha1OrNull;
        this.srcLastModifiedMillisOrNull = srcLastModifiedMillisOrNull;
    }



    public static B2ContentSource build(byte[] bytes) {
        return builder(bytes).build();
    }

    public static Builder builder(byte[] bytes) {
        return new Builder(bytes);
    }


    @Override
    public String getSha1OrNull() throws IOException {
        return sha1OrNull;
    }

    @Override
    public Long getSrcLastModifiedMillisOrNull() throws IOException {
        return srcLastModifiedMillisOrNull;
    }

    @Override
    public long getContentLength() throws IOException {
        return source.length;
    }

    @Override
    public InputStream createInputStream() throws IOException {
        return new ByteArrayInputStream(source);
    }

    public static class Builder {
        private final byte[] source;
        private String sha1OrNull;
        private Long srcLastModifiedMillisOrNull;


        public Builder(byte[] source) {
            this.source = source;
        }

        public Builder setSha1OrNull(String sha1OrNull) {
            this.sha1OrNull = sha1OrNull;
            return this;
        }

        public Builder setSrcLastModifiedMillisOrNull(Long srcLastModifiedMillisOrNull) {
            this.srcLastModifiedMillisOrNull = srcLastModifiedMillisOrNull;
            return this;
        }

        public B2ContentSource build() {
            return new B2ByteArrayContentSource(
                    source,
                    sha1OrNull,
                    srcLastModifiedMillisOrNull
            );
        }
    }
}
