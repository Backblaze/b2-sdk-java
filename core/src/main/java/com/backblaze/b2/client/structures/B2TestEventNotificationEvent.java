/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

/**
 * The individual event notification for a test event.
 */
public class B2TestEventNotificationEvent {

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

    @B2Json.constructor
    public B2TestEventNotificationEvent(String accountId,
                                        String bucketId,
                                        String bucketName,
                                        long eventTimestamp,
                                        String eventType,
                                        int eventVersion,
                                        String matchedRuleName) {
        this.accountId = accountId;
        this.bucketId = bucketId;
        this.bucketName = bucketName;
        this.eventTimestamp = eventTimestamp;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.matchedRuleName = matchedRuleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2TestEventNotificationEvent that = (B2TestEventNotificationEvent) o;
        return eventTimestamp == that.eventTimestamp &&
                eventVersion == that.eventVersion &&
                accountId.equals(that.accountId) &&
                bucketId.equals(that.bucketId) &&
                bucketName.equals(that.bucketName) &&
                eventType.equals(that.eventType) &&
                matchedRuleName.equals(that.matchedRuleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId,
                bucketId,
                bucketName,
                eventTimestamp,
                eventType,
                eventVersion,
                matchedRuleName
        );
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

    @Override
    public String toString() {
        return "B2TestEventNotificationEvent{" +
                "accountId='" + accountId + '\'' +
                ", bucketId='" + bucketId + '\'' +
                ", bucketName='" + bucketName + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                ", eventType='" + eventType + '\'' +
                ", eventVersion=" + eventVersion +
                ", matchedRuleName='" + matchedRuleName + '\'' +
                '}';
    }
}
