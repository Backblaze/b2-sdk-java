/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Objects;

/**
 * The notification that Backblaze sends when object events occur.
 */
public class B2EventNotification {

    @B2Json.required
    private final List<B2EventNotificationEvent> events;

    @B2Json.constructor
    public B2EventNotification(List<B2EventNotificationEvent> events) {
        this.events = events;
    }

    /**
     * A list of object events.
     */
    public List<B2EventNotificationEvent> getEvents() {
        return events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof B2EventNotification)) return false;
        B2EventNotification that = (B2EventNotification) o;
        return Objects.equals(events, that.events);
    }

    @Override
    public int hashCode() {
        return Objects.hash(events);
    }

    @Override
    public String toString() {
        return "B2EventNotification{" +
                "events=" + events +
                '}';
    }
}
