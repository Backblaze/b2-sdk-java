/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;

import java.util.Objects;

/**
 * One rule about when to delete files in a bucket.
 */
public class B2LifecycleRule {
    /**
     * The prefix that specifies what files this rule applies to.
     * Always set.  "" means all files.
     */
    @B2Json.required
    private final String fileNamePrefix;

    /**
     * How many days from the time a file version is uploaded until it gets hidden.
     * Null means never hide.
     */
    @B2Json.optional
    private final Integer daysFromUploadingToHiding;

    /**
     * How many days from when a file version is hidden (either by uploading a newer
     * version or by explicitly hiding it) until it gets deleted.
     * Null means never delete.
     */
    @B2Json.optional
    private final Integer daysFromHidingToDeleting;

    public static Builder builder(String fileNamePrefix) {
        return new Builder(fileNamePrefix);
    }

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }

    public Integer getDaysFromUploadingToHiding() {
        return daysFromUploadingToHiding;
    }

    public Integer getDaysFromHidingToDeleting() {
        return daysFromHidingToDeleting;
    }

    /**
     * Initializes a new, immutable rule.
     */
    @B2Json.constructor(params = "fileNamePrefix, daysFromUploadingToHiding, daysFromHidingToDeleting")
    private B2LifecycleRule(String fileNamePrefix,
                            Integer daysFromUploadingToHiding,
                            Integer daysFromHidingToDeleting) {
        B2Preconditions.checkArgument(fileNamePrefix != null, "fileNamePrefix must not be null");
        B2Preconditions.checkArgument(isNullOrPositive(daysFromUploadingToHiding), "daysFromUploadingToHiding must be positive");
        B2Preconditions.checkArgument(isNullOrPositive(daysFromHidingToDeleting), "daysFromHidingToDeleting must be positive");

        this.daysFromUploadingToHiding = daysFromUploadingToHiding;
        this.daysFromHidingToDeleting = daysFromHidingToDeleting;
        this.fileNamePrefix = fileNamePrefix;
    }

    @Override
    public String toString() {
        return String.format(
                "%s:%s:%s",
                fileNamePrefix,
                daysFromUploadingToHiding,
                daysFromHidingToDeleting
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2LifecycleRule that = (B2LifecycleRule) o;
        return Objects.equals(fileNamePrefix, that.fileNamePrefix) &&
                Objects.equals(daysFromUploadingToHiding, that.daysFromUploadingToHiding) &&
                Objects.equals(daysFromHidingToDeleting, that.daysFromHidingToDeleting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileNamePrefix, daysFromUploadingToHiding, daysFromHidingToDeleting);
    }

    /**
     * Returns true iff n is null or positive.
     */
    private static boolean isNullOrPositive(Integer n) {
        return (n == null) || (n > 0);
    }

    public static class Builder {
        private final String fileNamePrefix;
        private Integer daysFromUploadingToHiding;
        private Integer daysFromHidingToDeleting;

        public Builder(String fileNamePrefix) {
            this.fileNamePrefix = fileNamePrefix;
        }

        public Builder setDaysFromUploadingToHiding(Integer daysFromUploadingToHiding) {
            this.daysFromUploadingToHiding = daysFromUploadingToHiding;
            return this;
        }

        public Builder setDaysFromHidingToDeleting(Integer daysFromHidingToDeleting) {
            this.daysFromHidingToDeleting = daysFromHidingToDeleting;
            return this;
        }

        public B2LifecycleRule build() {
            return new B2LifecycleRule(fileNamePrefix, daysFromUploadingToHiding, daysFromHidingToDeleting);
        }
    }
}

