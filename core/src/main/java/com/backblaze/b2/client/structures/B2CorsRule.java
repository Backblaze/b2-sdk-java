/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class B2CorsRule {
    @B2Json.required
    private final String corsRuleName;
    @B2Json.required
    private final List<String> allowedOrigins;
    @B2Json.required
    private final Set<String> allowedOperations;
    @B2Json.optional
    private final List<String> allowedHeaders;
    @B2Json.optional
    private final List<String> exposeHeaders;
    @B2Json.required
    private final int maxAgeSeconds;

    @B2Json.constructor(params = "corsRuleName,allowedOrigins,allowedOperations,allowedHeaders,exposeHeaders,maxAgeSeconds")
    private B2CorsRule(String corsRuleName,
            List<String> allowedOrigins,
            Set<String> allowedOperations,
            List<String> allowedHeaders,
            List<String> exposeHeaders,
            int maxAgeSeconds) {

        this.corsRuleName = corsRuleName;
        this.allowedOrigins = allowedOrigins;
        this.allowedOperations = allowedOperations;
        this.allowedHeaders = allowedHeaders;
        this.exposeHeaders = exposeHeaders;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public String getCorsRuleName() { return corsRuleName; }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public Set<String> getAllowedOperations() {
        return allowedOperations;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public List<String> getExposeHeaders() {
        return exposeHeaders;
    }

    public int getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public static Builder builder(String corsRuleName, List<String> allowedOrigins, Set<String> allowedOperations, int maxAgeSeconds) {
        return new Builder(corsRuleName, allowedOrigins, allowedOperations, maxAgeSeconds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2CorsRule corsRule = (B2CorsRule) o;
        return corsRuleName.equals(corsRule.corsRuleName) &&
                maxAgeSeconds == corsRule.maxAgeSeconds &&
                Objects.equals(allowedOrigins, corsRule.allowedOrigins) &&
                Objects.equals(allowedOperations, corsRule.allowedOperations) &&
                Objects.equals(getAllowedHeaders(), corsRule.getAllowedHeaders()) &&
                Objects.equals(getExposeHeaders(), corsRule.getExposeHeaders());
    }

    @Override
    public int hashCode() {
        return Objects.hash(corsRuleName, allowedOrigins, allowedOperations, allowedHeaders, exposeHeaders, maxAgeSeconds);
    }

    @Override
    public String toString() {
        return "CorsRule{" +
                "corsRuleName=" + corsRuleName +
                ", allowedOrigin=" + allowedOrigins +
                ", allowedOperations=" + allowedOperations +
                ", allowedHeaders=" + allowedHeaders +
                ", exposeHeaders=" + exposeHeaders +
                ", maxAgeSeconds=" + maxAgeSeconds +
                '}';
    }

    public static class Builder {
        private String corsRuleName;
        private List<String> allowedOrigins;
        private Set<String> allowedOperations;
        private List<String> allowedHeaders;
        private List<String> exposeHeaders;
        private int maxAgeSeconds;

        Builder(String corsRuleName, List<String> allowedOrigins, Set<String> allowedOperations, int maxAgeSeconds) {
            this.corsRuleName = corsRuleName;
            this.allowedOrigins = allowedOrigins;
            this.allowedOperations = allowedOperations;
            this.allowedHeaders = null;
            this.exposeHeaders = null;
            this.maxAgeSeconds = maxAgeSeconds;
        }

        public Builder setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
            return this;
        }

        public Builder setExposeHeaders(List<String> exposeHeaders) {
            this.exposeHeaders = exposeHeaders;
            return this;
        }

        public B2CorsRule build() {
            return new B2CorsRule(corsRuleName, allowedOrigins, allowedOperations, allowedHeaders, exposeHeaders, maxAgeSeconds);
        }
    }
}
