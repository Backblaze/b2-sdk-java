/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.TreeSet;

/**
 * One rule about under what condition(s) to send notifications for events in a bucket.
 */
public class B2EventNotificationRule implements Comparable<B2EventNotificationRule> {
    private static final Comparator<B2EventNotificationRule> COMPARATOR = Comparator.comparing(B2EventNotificationRule::getName)
            .thenComparing(rule -> String.join(",", rule.getEventTypes()))
            .thenComparing(B2EventNotificationRule::getObjectNamePrefix)
            .thenComparing(B2EventNotificationRule::getTargetConfiguration, B2EventNotificationRule::compareTargetConfigurations)
            .thenComparing(B2EventNotificationRule::isEnabled)
            .thenComparing(B2EventNotificationRule::isSuspended)
            .thenComparing(B2EventNotificationRule::getSuspensionReason);

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
     * Indicates if the rule is suspended.
     */
    @B2Json.optional
    private final Boolean isSuspended;

    /**
     * If isSuspended is true, specifies the reason the rule was
     * suspended.
     */
    @B2Json.optional
    private final String suspensionReason;

    @B2Json.constructor
    public B2EventNotificationRule(String name,
            TreeSet<String> eventTypes,
            String objectNamePrefix,
            B2EventNotificationTargetConfiguration targetConfiguration,
            boolean isEnabled,
            Boolean isSuspended,
            String suspensionReason) {

        this.name = name;
        this.eventTypes = new TreeSet<>(eventTypes);
        this.objectNamePrefix = objectNamePrefix;
        this.targetConfiguration = targetConfiguration;
        this.isEnabled = isEnabled;
        this.isSuspended = isSuspended;
        this.suspensionReason = suspensionReason;
    }

    public B2EventNotificationRule(String name,
            TreeSet<String> eventTypes,
            String objectNamePrefix,
            B2EventNotificationTargetConfiguration targetConfiguration,
            boolean isEnabled) {
        this(name, eventTypes, objectNamePrefix, targetConfiguration, isEnabled, null, null);
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
        final B2EventNotificationRule that = (B2EventNotificationRule) o;
        return isEnabled == that.isEnabled &&
                name.equals(that.name) &&
                eventTypes.equals(that.eventTypes) &&
                objectNamePrefix.equals(that.objectNamePrefix) &&
                targetConfiguration.equals(that.targetConfiguration) &&
                Objects.equals(isSuspended, that.isSuspended) &&
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
        return "B2EventNotificationRule{" +
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
    public int compareTo(B2EventNotificationRule r) {
        return COMPARATOR.compare(this, r);
    }

    private static int compareTargetConfigurations(B2EventNotificationTargetConfiguration first,
                                                   B2EventNotificationTargetConfiguration second) {
        if (first == second) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }
        if (first instanceof B2WebhookConfiguration && second instanceof B2WebhookConfiguration) {
            return compareWebhookConfigurations((B2WebhookConfiguration) first, (B2WebhookConfiguration) second);
        }

        final int classComparison = first.getClass().getName().compareTo(second.getClass().getName());
        if (classComparison != 0) {
            return classComparison;
        }
        if (first.equals(second)) {
            return 0;
        }
        return first.toString().compareTo(second.toString());
    }

    private static int compareWebhookConfigurations(B2WebhookConfiguration first,
                                                    B2WebhookConfiguration second) {
        int comparison = first.getUrl().compareTo(second.getUrl());
        if (comparison != 0) {
            return comparison;
        }

        comparison = compareCustomHeaders(first.getCustomHeaders(), second.getCustomHeaders());
        if (comparison != 0) {
            return comparison;
        }

        comparison = compareNullableStrings(first.getHmacSha256SigningSecret(), second.getHmacSha256SigningSecret());
        if (comparison != 0) {
            return comparison;
        }

        return compareNullableIntegers(first.getMaxEventsPerBatch(), second.getMaxEventsPerBatch());
    }

    private static int compareCustomHeaders(TreeSet<B2WebhookCustomHeader> first,
                                            TreeSet<B2WebhookCustomHeader> second) {
        if (first == second) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }

        final Iterator<B2WebhookCustomHeader> firstIterator = first.iterator();
        final Iterator<B2WebhookCustomHeader> secondIterator = second.iterator();
        while (firstIterator.hasNext() && secondIterator.hasNext()) {
            final int comparison = firstIterator.next().compareTo(secondIterator.next());
            if (comparison != 0) {
                return comparison;
            }
        }
        return Boolean.compare(firstIterator.hasNext(), secondIterator.hasNext());
    }

    private static int compareNullableStrings(String first, String second) {
        if (first == second) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }
        return first.compareTo(second);
    }

    private static int compareNullableIntegers(Integer first, Integer second) {
        if (first == second) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }
        return first.compareTo(second);
    }
}
