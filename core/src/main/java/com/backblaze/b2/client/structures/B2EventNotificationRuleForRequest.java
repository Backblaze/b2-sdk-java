/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * One rule about under what condition(s) to send notifications for events in a bucket.
 */
public class B2EventNotificationRuleForRequest implements Comparable<B2EventNotificationRuleForRequest> {
    private static final Comparator<B2EventNotificationRuleForRequest> COMPARATOR = Comparator.comparing(B2EventNotificationRuleForRequest::getName)
            .thenComparing(rule -> String.join(",", rule.getEventTypes()))
            .thenComparing(B2EventNotificationRuleForRequest::getObjectNamePrefix)
            .thenComparing(rule -> rule.getTargetConfiguration().toString())
            .thenComparing(B2EventNotificationRuleForRequest::isEnabled);

    /**
     * A name for identifying the rule. Names must be unique within a bucket.
     * The length requirements correspond to the bucket minimum and maximum lengths,
     * which are 6 and 63 characters respectively at the time of this writing.
     */
    @B2Json.required
    private final String name;

    /**
     * The Set of Strings identifying the applicable event types for this rule.
     * Event types support the wildcard character "*" in the last component only.
     * However, event types must not overlap.  For example, the Set must
     * NOT contain "b2:ObjectCreated:Upload" and "b2:ObjectCreated:*".
     */
    @B2Json.required
    private final TreeSet<String> eventTypes;

    /**
     * The prefix that specifies what object(s) this rule applies to.
     * Always set.  "" means all objects.
     */
    @B2Json.required
    private final String objectNamePrefix;

    /**
     * The target configuration for the event notification.
     */
    @B2Json.required
    private final B2EventNotificationTargetConfigurationForRequest targetConfiguration;

    /**
     * Indicates if the rule is enabled.
     */
    @B2Json.required
    private final boolean isEnabled;

    @B2Json.constructor
    public B2EventNotificationRuleForRequest(String name,
                                             TreeSet<String> eventTypes,
                                             String objectNamePrefix,
                                             B2EventNotificationTargetConfigurationForRequest targetConfiguration,
                                             boolean isEnabled) {

        this.name = name;
        this.eventTypes = new TreeSet<>(eventTypes);
        this.objectNamePrefix = objectNamePrefix;
        this.targetConfiguration = targetConfiguration;
        this.isEnabled = isEnabled;
    }

    public String getName() {
        return name;
    }

    public TreeSet<String> getEventTypes() {
        return eventTypes;
    }

    public String getObjectNamePrefix() {
        return objectNamePrefix;
    }

    public B2EventNotificationTargetConfigurationForRequest getTargetConfiguration() {
        return targetConfiguration;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2EventNotificationRuleForRequest that = (B2EventNotificationRuleForRequest) o;
        return isEnabled == that.isEnabled &&
                name.equals(that.name) &&
                eventTypes.equals(that.eventTypes) &&
                objectNamePrefix.equals(that.objectNamePrefix) &&
                targetConfiguration.equals(that.targetConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                eventTypes,
                objectNamePrefix,
                targetConfiguration,
                isEnabled
        );
    }

    @Override
    public String toString() {
        return "B2EventNotificationRuleForRequest{" +
                "name='" + name + '\'' +
                ", eventTypes=" + eventTypes +
                ", objectNamePrefix='" + objectNamePrefix + '\'' +
                ", targetConfiguration=" + targetConfiguration +
                ", isEnabled=" + isEnabled +
                '}';
    }

    /**
     * Rules are sorted by name first, and then additional attributes if necessary.
     */
    @Override
    public int compareTo(B2EventNotificationRuleForRequest r) {
        return COMPARATOR.compare(this, r);
    }

    /**
     * Convenience method to convert a List of B2EventNotificationRuleForResponse to a List of
     * B2EventNotificationRuleForRequest.
     * @param rules the List of B2EventNotificationRuleForResponse to convert
     * @return the converted List of B2EventNotificationRuleForRequest or null if null was supplied as argument.
     */
    public static List<B2EventNotificationRuleForRequest> convertToListOfB2EventNotificationRuleForRequest(
            List<B2EventNotificationRuleForResponse> rules) {

        if (rules == null) {
            return null;
        }

        final List<B2EventNotificationRuleForRequest> requestList = new ArrayList<>();
        for (B2EventNotificationRuleForResponse response : rules) {
            requestList.add(convertToB2EventNotificationForRequest(response));
        }
        return requestList;
    }

    /**
     * Convenience method to convert a B2EventNotificationRuleForResponse to a
     * B2EventNotificationRuleForRequest.
     * @param rule the B2EventNotificationRuleForResponse to convert
     * @return the converted B2EventNotificationRuleForRequest or null if null was supplied as argument.
     */
    public static B2EventNotificationRuleForRequest convertToB2EventNotificationForRequest(
            B2EventNotificationRuleForResponse rule) {

        if (rule == null) {
            return null;
        }

        return new B2EventNotificationRuleForRequest(
                rule.getName(),
                rule.getEventTypes(),
                rule.getObjectNamePrefix(),
                B2WebhookConfigurationForRequest.convertToB2EventNotificationTargetConfigurationForRequest(
                        rule.getTargetConfiguration()
                ),
                rule.isEnabled()
        );
    }

}
