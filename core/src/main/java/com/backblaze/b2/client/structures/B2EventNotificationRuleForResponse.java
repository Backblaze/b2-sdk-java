/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;
import com.backblaze.b2.util.B2StringUtil;

import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;

/**
 * One rule about under what condition(s) to send notifications for events in a bucket.
 */
public class B2EventNotificationRuleForResponse implements Comparable<B2EventNotificationRuleForResponse> {
    private static final Comparator<B2EventNotificationRuleForResponse> COMPARATOR = Comparator.comparing(B2EventNotificationRuleForResponse::getName)
            .thenComparing(rule -> String.join(",", rule.getEventTypes()))
            .thenComparing(B2EventNotificationRuleForResponse::getObjectNamePrefix)
            .thenComparing(rule -> rule.getTargetConfiguration().toString())
            .thenComparing(B2EventNotificationRuleForResponse::isEnabled)
            .thenComparing(B2EventNotificationRuleForResponse::isSuspended)
            .thenComparing(B2EventNotificationRuleForResponse::getSuspensionReason);

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
    private final B2EventNotificationTargetConfigurationForResponse targetConfiguration;

    /**
     * Indicates if the rule is enabled.
     */
    @B2Json.required
    private final boolean isEnabled;

    /**
     * Indicates if the rule is suspended.
     */
    @B2Json.required
    private final boolean isSuspended;

    /**
     * If isSuspended is true, specifies the reason the rule was
     * suspended.
     */
    @B2Json.optional
    private final String suspensionReason;

    @B2Json.constructor
    public B2EventNotificationRuleForResponse(String name,
                                              TreeSet<String> eventTypes,
                                              String objectNamePrefix,
                                              B2EventNotificationTargetConfigurationForResponse targetConfiguration,
                                              boolean isEnabled,
                                              boolean isSuspended,
                                              String suspensionReason) {

        B2Preconditions.checkArgument(
                !isSuspended || !B2StringUtil.isEmpty(suspensionReason),
                "A suspension reason is required if isSuspended is true"
        );

        this.name = name;
        this.eventTypes = new TreeSet<>(eventTypes);
        this.objectNamePrefix = objectNamePrefix;
        this.targetConfiguration = targetConfiguration;
        this.isEnabled = isEnabled;
        this.isSuspended = isSuspended;
        this.suspensionReason = suspensionReason;
    }

    public B2EventNotificationRuleForResponse(String name,
                                              TreeSet<String> eventTypes,
                                              String objectNamePrefix,
                                              B2EventNotificationTargetConfigurationForResponse targetConfiguration,
                                              boolean isEnabled) {
        this(name, eventTypes, objectNamePrefix, targetConfiguration, isEnabled, false, null);
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

    public B2EventNotificationTargetConfigurationForResponse getTargetConfiguration() {
        return targetConfiguration;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isSuspended() {
        return isSuspended;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2EventNotificationRuleForResponse that = (B2EventNotificationRuleForResponse) o;
        return isEnabled == that.isEnabled &&
                isSuspended == that.isSuspended &&
                name.equals(that.name) &&
                eventTypes.equals(that.eventTypes) &&
                objectNamePrefix.equals(that.objectNamePrefix) &&
                targetConfiguration.equals(that.targetConfiguration) &&
                Objects.equals(suspensionReason, that.suspensionReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                eventTypes,
                objectNamePrefix,
                targetConfiguration,
                isEnabled,
                isSuspended,
                suspensionReason
        );
    }

    @Override
    public String toString() {
        return "B2EventNotificationRuleForResponse{" +
                "name='" + name + '\'' +
                ", eventTypes=" + eventTypes +
                ", objectNamePrefix='" + objectNamePrefix + '\'' +
                ", targetConfiguration=" + targetConfiguration +
                ", isEnabled=" + isEnabled +
                ", isSuspended=" + isSuspended +
                ", suspensionReason='" + suspensionReason + '\'' +
                '}';
    }

    /**
     * Rules are sorted by name first, and then additional attributes if necessary.
     */
    @Override
    public int compareTo(B2EventNotificationRuleForResponse r) {
        return COMPARATOR.compare(this, r);
    }
}
