/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;
import java.util.TreeSet;

/**
 * Webhook destination for B2EventNotificationRule
 */
public class B2WebhookConfiguration extends B2EventNotificationTargetConfiguration {
    /**
     * The URL endpoint for the webhook, including the protocol, which
     * must be "https://".
     */
    @B2Json.required
    private final String url;

    @B2Json.optional
    private final TreeSet<B2WebhookCustomHeader> customHeaders;

    @B2Json.optional
    private final String hmacSha256SigningSecret;

    /**
     * An optional maximum number of events to batch into a single webhook request.
     */
    @B2Json.optional(omitNull = true)
    private final Integer maxEventsPerBatch;

    @B2Json.constructor
    public B2WebhookConfiguration(String url,
            TreeSet<B2WebhookCustomHeader> customHeaders,
            String hmacSha256SigningSecret,
            Integer maxEventsPerBatch) {
        B2Preconditions.checkArgument(
                url != null && url.startsWith("https://"),
                "The protocol for the url must be https://"
        );
        B2Preconditions.checkArgument(
                maxEventsPerBatch == null || (maxEventsPerBatch > 0 && maxEventsPerBatch <= 50),
                "The events per batch must be between 1 and 50"
        );

        this.url = url;
        this.customHeaders = customHeaders;
        this.hmacSha256SigningSecret = hmacSha256SigningSecret;
        this.maxEventsPerBatch = maxEventsPerBatch;
    }

    public B2WebhookConfiguration(String url) {
        this(url, null, null, null);
    }

    public B2WebhookConfiguration(String url, TreeSet<B2WebhookCustomHeader> customHeaders) {
        this(url, customHeaders, null, null);
    }

    public B2WebhookConfiguration(String url, String hmacSha256SigningSecret) {
        this(url, null, hmacSha256SigningSecret, null);
    }

    public String getUrl() {
        return url;
    }

    public TreeSet<B2WebhookCustomHeader> getCustomHeaders() {
        if (customHeaders == null) {
            return null;
        }
        return new TreeSet<>(customHeaders);
    }

    public String getHmacSha256SigningSecret() {
        return hmacSha256SigningSecret;
    }

    public Integer getMaxEventsPerBatch() {
        return maxEventsPerBatch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2WebhookConfiguration that = (B2WebhookConfiguration) o;
        return url.equals(that.url) &&
                Objects.equals(customHeaders, that.customHeaders) &&
                Objects.equals(hmacSha256SigningSecret, that.hmacSha256SigningSecret) &&
                Objects.equals(maxEventsPerBatch, that.maxEventsPerBatch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, customHeaders, hmacSha256SigningSecret);
    }

    @Override
    public String toString() {
        return "B2WebhookConfiguration{" +
                "url='" + url + '\'' +
                ", customHeaders=" + customHeaders +
                ", hmacSha256SigningSecret='" + hmacSha256SigningSecret + '\'' +
                '}';
    }
}
