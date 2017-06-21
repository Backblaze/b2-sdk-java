/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.contentSources;

import com.backblaze.b2.util.B2Preconditions;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * B2HeadersImpl implements the B2Headers interface.
 */
public class B2HeadersImpl implements B2Headers {
    private Map<String,String> pairs;

    private B2HeadersImpl(Map<String, String> pairs) {
        this.pairs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.pairs.putAll(pairs);
        B2Preconditions.checkArgument(this.pairs.size() == pairs.size(),
                "argument contained keys that only differed by case!");
    }

    @Override
    public Collection<String> getNames() {
        return Collections.unmodifiableCollection(pairs.keySet());
    }

    @Override
    public String getValueOrNull(String name) {
        return pairs.get(name);
    }

    /**
     * @return a new builder with no headers set yet.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param headersOrNull if non-null, the headers to prepopulate in the builder.
     * @return a new builder.  if headersOrNull != null, the values from those
     *         headers will have already been set on the returned builder.
     */
    public static Builder builder(B2Headers headersOrNull) {
        final Builder builder = new Builder();
        if (headersOrNull != null) {
            for (String name : headersOrNull.getNames()) {
                builder.set(name, headersOrNull.getValueOrNull(name));
            }
        }
        return builder;
    }

    public static class Builder {
        private final Map<String,String> pairs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public Builder set(String name, String value) {
            B2Preconditions.checkArgument(!pairs.containsKey(name), "already have a value for " + name);
            pairs.put(name, value);
            return this;
        }

        public B2HeadersImpl build() {
            return new B2HeadersImpl(pairs);
        }
    }
}
