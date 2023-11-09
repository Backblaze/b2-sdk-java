/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class B2SetBucketNotificationRulesResponse {

    @B2Json.required
    private final String bucketId;

    @B2Json.required
    private final List<B2EventNotificationRule> eventNotificationRules;

    @B2Json.constructor
    public B2SetBucketNotificationRulesResponse(String bucketId,
                                                List<B2EventNotificationRule> eventNotificationRules) {
        this.bucketId = bucketId;
        this.eventNotificationRules = eventNotificationRules;
    }

    public String getBucketId() {
        return bucketId;
    }

    public List<B2EventNotificationRule> getEventNotificationRules() {
        return new ArrayList<>(eventNotificationRules);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2SetBucketNotificationRulesResponse that = (B2SetBucketNotificationRulesResponse) o;
        return bucketId.equals(that.bucketId) && eventNotificationRules.equals(that.eventNotificationRules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketId, eventNotificationRules);
    }

    @Override
    public String toString() {
        return "B2SetBucketNotificationRulesResponse{" +
                "bucketId='" + bucketId + '\'' +
                ", eventNotificationRules=" + eventNotificationRules +
                '}';
    }

    public static B2SetBucketNotificationRulesResponse.Builder builder(String bucketId,
                                                                       List<B2EventNotificationRule> eventNotificationRules) {
        return new B2SetBucketNotificationRulesResponse.Builder(bucketId, eventNotificationRules);
    }

    public static class Builder {
        private final String bucketId;
        private final List<B2EventNotificationRule> eventNotificationRules;

        public Builder(String bucketId,
                       List<B2EventNotificationRule> eventNotificationRules) {
            this.bucketId = bucketId;
            this.eventNotificationRules = eventNotificationRules;
        }

        public B2SetBucketNotificationRulesResponse build() {
            return new B2SetBucketNotificationRulesResponse(bucketId, eventNotificationRules);
        }
    }
}
