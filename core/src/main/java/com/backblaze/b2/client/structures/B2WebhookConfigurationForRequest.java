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
 * Webhook destination for B2EventNotificationRuleForRequest
 */
public class B2WebhookConfigurationForRequest extends B2EventNotificationTargetConfigurationForRequest {
    /**
     * The URL endpoint for the webhook, including the protocol, which
     * must be "https://".
     */
    @B2Json.required
    private final String url;

    @B2Json.optional
    private final TreeSet<B2CustomHeaderForRequest> customHeaders;

    @B2Json.constructor
    public B2WebhookConfigurationForRequest(String url,
                                            TreeSet<B2CustomHeaderForRequest> customHeaders) {
        B2Preconditions.checkArgument(
                url != null && url.startsWith("https://"),
                "The protocol for the url must be https://"
        );

        this.url = url;
        this.customHeaders = customHeaders;
    }

    public B2WebhookConfigurationForRequest(String url) {
        this(url, null);
    }

    public String getUrl() {
        return url;
    }

    public TreeSet<B2CustomHeaderForRequest> getCustomHeaders() {
        if (customHeaders == null) {
            return null;
        }
        return new TreeSet<>(customHeaders);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2WebhookConfigurationForRequest that = (B2WebhookConfigurationForRequest) o;
        return url.equals(that.url) &&
                Objects.equals(customHeaders, that.customHeaders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, customHeaders);
    }

    @Override
    public String toString() {
        return "B2WebhookConfigurationForRequest{" +
                "url='" + url + '\'' +
                ", customHeaders=" + customHeaders +
                '}';
    }
}
