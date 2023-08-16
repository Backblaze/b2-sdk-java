/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

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

    @B2Json.constructor
    public B2WebhookConfiguration(String url) {
        B2Preconditions.checkArgument(
                url != null && url.startsWith("https://"),
                "The protocol for the url must be https://"
        );

        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2WebhookConfiguration that = (B2WebhookConfiguration) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "B2WebhookConfiguration{" +
                "url='" + url + '\'' +
                '}';
    }
}
