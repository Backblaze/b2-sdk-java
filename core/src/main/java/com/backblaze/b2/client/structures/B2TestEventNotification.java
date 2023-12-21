/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Objects;

/**
 * The notification that Backblaze sends for test events.
 */
public class B2TestEventNotification {

    @B2Json.required
    private final List<B2TestEventNotificationEvent> events;

    @B2Json.constructor
    public B2TestEventNotification(List<B2TestEventNotificationEvent> events) {
        this.events = events;
    }

    /**
     * A list of test events.
     */
    public List<B2TestEventNotificationEvent> getEvents() {
        return events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof B2TestEventNotification)) return false;
        final B2TestEventNotification that = (B2TestEventNotification) o;
        return Objects.equals(events, that.events);
    }

    @Override
    public int hashCode() {
        return Objects.hash(events);
    }

    @Override
    public String toString() {
        return "B2TestEventNotification{" +
                "events=" + events +
                '}';
    }
}
