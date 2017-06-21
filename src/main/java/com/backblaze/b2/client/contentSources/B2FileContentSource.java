/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentSources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class B2FileContentSource implements B2ContentSource {
    private final File source;
    private final String sha1OrNull;

    public static Builder builder(File source) {
        return new Builder(source);
    }

    public static B2FileContentSource build(File source) {
        return builder(source).build();
    }

    private B2FileContentSource(File source,
                                String sha1) {
        this.source = source;
        this.sha1OrNull = sha1;
    }

    @Override
    public String getSha1OrNull() throws IOException {
        return sha1OrNull;
    }

    @Override
    public Long getSrcLastModifiedMillisOrNull() throws IOException {
        return source.lastModified();
    }

    @Override
    public long getContentLength() throws IOException {
        return source.length();
    }

    @Override
    public InputStream createInputStream() throws IOException {
        return new FileInputStream(source);
    }

    public static class Builder {
        private final File source;
        private String sha1;

        private Builder(File source) {
            this.source = source;
        }

        public Builder setSha1(String sha1) {
            this.sha1 = sha1;
            return this;
        }

        public B2FileContentSource build() {
            return new B2FileContentSource(source, sha1);
        }
    }
}
