/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class B2SetBucketNotificationRulesRequest {

    @B2Json.required
    private final String bucketId;

    @B2Json.required
    private final List<B2EventNotificationRuleForRequest> eventNotificationRules;

    @B2Json.constructor
    private B2SetBucketNotificationRulesRequest(String bucketId,
                                                List<B2EventNotificationRuleForRequest> eventNotificationRules) {
        this.bucketId = bucketId;
        this.eventNotificationRules = eventNotificationRules;
    }

    public String getBucketId() {
        return bucketId;
    }

    public List<B2EventNotificationRuleForRequest> getEventNotificationRules() {
        return new ArrayList<>(eventNotificationRules);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2SetBucketNotificationRulesRequest that = (B2SetBucketNotificationRulesRequest) o;
        return bucketId.equals(that.bucketId) && eventNotificationRules.equals(that.eventNotificationRules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketId, eventNotificationRules);
    }

    @Override
    public String toString() {
        return "B2SetBucketNotificationRulesRequest{" +
                "bucketId='" + bucketId + '\'' +
                ", eventNotificationRules=" + eventNotificationRules +
                '}';
    }

    public static B2SetBucketNotificationRulesRequest.Builder builder(String bucketId,
                                                                      List<B2EventNotificationRuleForRequest> eventNotificationRules) {
        return new B2SetBucketNotificationRulesRequest.Builder(bucketId, eventNotificationRules);
    }

    public static class Builder {
        private final String bucketId;

        private final List<B2EventNotificationRuleForRequest> eventNotificationRules;

        public Builder(String bucketId,
                       List<B2EventNotificationRuleForRequest> eventNotificationRules) {
            this.bucketId = bucketId;
            this.eventNotificationRules = eventNotificationRules;
        }

        public B2SetBucketNotificationRulesRequest build() {
            return new B2SetBucketNotificationRulesRequest(bucketId, eventNotificationRules);
        }
    }
}
