/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

/**
 * The individual event notification for an object.
 */
public class B2EventNotificationEvent {

    @B2Json.required
    private final long eventTimestamp;

    @B2Json.required
    private final String eventType;

    @B2Json.required
    private final String objectName;

    @B2Json.required
    private final long objectSize;

    @B2Json.required
    private final String objectVersionId;

    @B2Json.constructor
    public B2EventNotificationEvent(long eventTimestamp, String eventType, String objectName, long objectSize, String objectVersionId) {
        this.eventTimestamp = eventTimestamp;
        this.eventType = eventType;
        this.objectName = objectName;
        this.objectSize = objectSize;
        this.objectVersionId = objectVersionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof B2EventNotificationEvent)) return false;
        B2EventNotificationEvent that = (B2EventNotificationEvent) o;
        return eventTimestamp == that.eventTimestamp &&
                objectSize == that.objectSize &&
                Objects.equals(eventType, that.eventType) &&
                Objects.equals(objectName, that.objectName) &&
                Objects.equals(objectVersionId, that.objectVersionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventTimestamp, eventType, objectName, objectSize, objectVersionId);
    }

    /**
     * The UTC time when this event was generated. It is a base 10 number of milliseconds since midnight, January 1, 1970 UTC.
     */
    public long getEventTimestamp() {
        return eventTimestamp;
    }

    /**
     * The event type of the event notification rule that corresponds to the event.
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * The name of the object that corresponds to the event.
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * The size of bytes of the object that corresponds to the event.  The objectSize would be 0 for hide markers.
     */
    public long getObjectSize() {
        return objectSize;
    }

    /**
     * The unique identifier for the version of the object that corresponds to the event.
     */
    public String getObjectVersionId() {
        return objectVersionId;
    }

    @Override
    public String toString() {
        return "B2EventNotificationEvent{" +
                "eventTimestamp=" + eventTimestamp +
                ", eventType='" + eventType + '\'' +
                ", objectName='" + objectName + '\'' +
                ", objectSize=" + objectSize +
                ", objectVersionId='" + objectVersionId + '\'' +
                '}';
    }
}
