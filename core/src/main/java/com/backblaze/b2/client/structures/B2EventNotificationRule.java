/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;

/**
 * One rule about under what condition(s) to send notifications for events in a bucket.
 */
public class B2EventNotificationRule implements Comparable<B2EventNotificationRule> {
    private static final Comparator<B2EventNotificationRule> COMPARATOR = Comparator.comparing(B2EventNotificationRule::getName)
            .thenComparing(rule -> String.join(",", rule.getEventTypes()))
            .thenComparing(B2EventNotificationRule::getObjectNamePrefix)
            .thenComparing(rule -> rule.getTargetConfiguration().toString())
            .thenComparing(rule -> String.valueOf(rule.isEnabled()))
            .thenComparing(B2EventNotificationRule::getDisabledReason);

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
    private final B2EventNotificationTargetConfiguration targetConfiguration;

    /**
     * Indicates if the rule is enabled.
     */
    @B2Json.required
    private final boolean isEnabled;

    /**
     * If isEnabled is false, specifies the reason the rule was
     * disabled.
     * Always set.  "" means not disabled.
     */
    @B2Json.required
    private final String disabledReason;

    @B2Json.constructor
    public B2EventNotificationRule(String name,
                                   TreeSet<String> eventTypes,
                                   String objectNamePrefix,
                                   B2EventNotificationTargetConfiguration targetConfiguration,
                                   boolean isEnabled,
                                   String disabledReason) {

        B2Preconditions.checkArgument(
                !isEnabled || "".equals(disabledReason),
                "disableReason must only have a value if isEnabled is false"
        );

        this.name = name;
        this.eventTypes = new TreeSet<>(eventTypes);
        this.objectNamePrefix = objectNamePrefix;
        this.targetConfiguration = targetConfiguration;
        this.isEnabled = isEnabled;
        this.disabledReason = disabledReason;
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

    public B2EventNotificationTargetConfiguration getTargetConfiguration() {
        return targetConfiguration;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public String getDisabledReason() {
        return disabledReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2EventNotificationRule that = (B2EventNotificationRule) o;
        return isEnabled == that.isEnabled &&
                name.equals(that.name) &&
                eventTypes.equals(that.eventTypes) &&
                objectNamePrefix.equals(that.objectNamePrefix) &&
                targetConfiguration.equals(that.targetConfiguration) &&
                disabledReason.equals(that.disabledReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, eventTypes, objectNamePrefix, targetConfiguration, isEnabled, disabledReason);
    }

    @Override
    public String toString() {
        return "B2EventNotificationRule{" +
                "name='" + name + '\'' +
                ", eventTypes=" + eventTypes +
                ", objectNamePrefix='" + objectNamePrefix + '\'' +
                ", targetConfiguration=" + targetConfiguration +
                ", isEnabled=" + isEnabled +
                ", disabledReason='" + disabledReason + '\'' +
                '}';
    }

    /**
     * Rules are sorted by name first, and then additional attributes if necessary.
     */
    @Override
    public int compareTo(B2EventNotificationRule r) {
        return COMPARATOR.compare(this, r);
    }
}
