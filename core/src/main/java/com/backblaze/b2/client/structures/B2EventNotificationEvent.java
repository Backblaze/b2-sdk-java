/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;
import com.backblaze.b2.util.B2StringUtil;

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
    String eventId;
    @B2Json.required
    private final int eventVersion;
    @B2Json.required
    private final String matchedRuleName;
    @B2Json.optional(omitNull = true)
    private final String objectName;
    @B2Json.optional(omitNull = true)
    private final Long objectSize;
    @B2Json.optional(omitNull = true)
    private final String objectVersionId;

    @B2Json.constructor
    public B2EventNotificationEvent(String accountId,
                                    String bucketId,
                                    String bucketName,
                                    long eventTimestamp,
                                    String eventType,
                                    String eventId,
                                    int eventVersion,
                                    String matchedRuleName,
                                    String objectName,
                                    Long objectSize,
                                    String objectVersionId) {

        if (!"b2:TestEvent".equals(eventType)) {
            B2Preconditions.checkArgument(!B2StringUtil.isEmpty(objectName), "objectName is required");
            B2Preconditions.checkArgument(!B2StringUtil.isEmpty(objectVersionId), "objectVersionId is required");
        } else {
            B2Preconditions.checkArgument(objectName == null, "objectName must be null for test events");
            B2Preconditions.checkArgument(objectSize == null, "objectSize must be null for test events");
            B2Preconditions.checkArgument(objectVersionId == null, "objectVersionId must be null for test events");
        }
        this.accountId = accountId;
        this.bucketId = bucketId;
        this.bucketName = bucketName;
        this.eventTimestamp = eventTimestamp;
        this.eventType = eventType;
        this.eventId = eventId;
        this.eventVersion = eventVersion;
        this.matchedRuleName = matchedRuleName;
        this.objectName = objectName;
        this.objectSize = objectSize;
        this.objectVersionId = objectVersionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2EventNotificationEvent that = (B2EventNotificationEvent) o;
        return eventTimestamp == that.eventTimestamp &&
                eventVersion == that.eventVersion &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(bucketId, that.bucketId) &&
                Objects.equals(bucketName, that.bucketName) &&
                Objects.equals(eventType, that.eventType) &&
                Objects.equals(eventId, that.eventId) &&
                Objects.equals(matchedRuleName, that.matchedRuleName) &&
                Objects.equals(objectName, that.objectName) &&
                Objects.equals(objectSize, that.objectSize) &&
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
                eventId,
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
     * The unique ID of the event.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * The name of the object that corresponds to the event.  This will be null for test events.
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * The size of bytes of the object that corresponds to the event.  The objectSize would be null for hide marker,
     * delete, and test events.
     */
    public Long getObjectSize() {
        return objectSize;
    }

    /**
     * The unique identifier for the version of the object that corresponds to the event.  This will be null
     * for test events.
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
