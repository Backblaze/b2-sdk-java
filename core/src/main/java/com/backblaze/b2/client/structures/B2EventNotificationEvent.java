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
    private final String accountId;
    @B2Json.required
    private final String bucketId;
    @B2Json.required
    private final String bucketName;
    @B2Json.required
    private final long eventTimestamp;
    @B2Json.required
    private final String eventType;
    @B2Json.required
    private final int eventVersion;
    @B2Json.required
    private final String matchedRuleName;
    @B2Json.required
    private final String objectName;
    @B2Json.required
    private final long objectSize;
    @B2Json.required
    private final String objectVersionId;

    @B2Json.constructor
    public B2EventNotificationEvent(String accountId,
                                    String bucketId,
                                    String bucketName,
                                    long eventTimestamp,
                                    String eventType,
                                    int eventVersion,
                                    String matchedRuleName,
                                    String objectName,
                                    long objectSize,
                                    String objectVersionId) {
        this.accountId = accountId;
        this.bucketId = bucketId;
        this.bucketName = bucketName;
        this.eventTimestamp = eventTimestamp;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.matchedRuleName = matchedRuleName;
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
                eventVersion == that.eventVersion &&
                objectSize == that.objectSize &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(bucketId, that.bucketId) &&
                Objects.equals(bucketName, that.bucketName) &&
                Objects.equals(eventType, that.eventType) &&
                Objects.equals(matchedRuleName, that.matchedRuleName) &&
                Objects.equals(objectName, that.objectName) &&
                Objects.equals(objectVersionId, that.objectVersionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                accountId,
                bucketId,
                bucketName,
                eventTimestamp,
                eventType,
                eventVersion,
                matchedRuleName,
                objectName,
                objectSize,
                objectVersionId);
    }

    /**
     * The version of the event notification payload.
     */
    public int getEventVersion() {
        return eventVersion;
    }

    /**
     * The name of the event notification rule the corresponds to the event.
     */
    public String getMatchedRuleName() {
        return matchedRuleName;
    }

    /**
     * The name of the bucket where the objects reside that corresponds to the event.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * The ID of the bucket where the objects reside that corresponds to the event.
     */
    public String getBucketId() {
        return bucketId;
    }

    /**
     * The ID of the account where the objects reside that corresponds to the event.
     */
    public String getAccountId() {
        return accountId;
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
                "accountId='" + accountId + '\'' +
                ", bucketId='" + bucketId + '\'' +
                ", bucketName='" + bucketName + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                ", eventType='" + eventType + '\'' +
                ", eventVersion=" + eventVersion +
                ", matchedRuleName='" + matchedRuleName + '\'' +
                ", objectName='" + objectName + '\'' +
                ", objectSize=" + objectSize +
                ", objectVersionId='" + objectVersionId + '\'' +
                '}';
    }
}