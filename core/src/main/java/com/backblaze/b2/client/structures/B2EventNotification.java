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
    private final int eventVersion;

    @B2Json.required
    private final String matchedRuleName;

    @B2Json.required
    private final String bucketName;

    @B2Json.required
    private final String bucketId;

    @B2Json.required
    private final String accountId;

    @B2Json.required
    private final List<B2EventNotificationEvent> events;

    @B2Json.constructor
    public B2EventNotification(int eventVersion,
                               String matchedRuleName,
                               String bucketName,
                               String bucketId,
                               String accountId,
                               List<B2EventNotificationEvent> events) {
        this.eventVersion = eventVersion;
        this.matchedRuleName = matchedRuleName;
        this.bucketName = bucketName;
        this.bucketId = bucketId;
        this.accountId = accountId;
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof B2EventNotification)) return false;
        B2EventNotification that = (B2EventNotification) o;
        return eventVersion == that.eventVersion &&
                Objects.equals(matchedRuleName, that.matchedRuleName) &&
                Objects.equals(bucketName, that.bucketName) &&
                Objects.equals(bucketId, that.bucketId) &&
                Objects.equals(accountId, that.accountId) &&
                Objects.equals(events, that.events);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventVersion, matchedRuleName, bucketName, bucketId, accountId, events);
    }

    @Override
    public String toString() {
        return "B2EventNotification{" +
                "eventVersion=" + eventVersion +
                ", matchedRuleName='" + matchedRuleName + '\'' +
                ", bucketName='" + bucketName + '\'' +
                ", bucketId='" + bucketId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", events=" + events +
                '}';
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
     * A list of object events.
     */
    public List<B2EventNotificationEvent> getEvents() {
        return events;
    }
}
