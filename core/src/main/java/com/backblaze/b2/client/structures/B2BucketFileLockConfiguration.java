/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2BucketFileLockConfiguration {

    // whether file lock is enabled on the bucket
    @B2Json.required
    private final boolean isFileLockEnabled;

    @B2Json.required
    private final B2BucketDefaultRetention defaultRetention;

    @B2Json.constructor(params = "isFileLockEnabled, defaultRetention")
    public B2BucketFileLockConfiguration(boolean isFileLockEnabled,
                                         B2BucketDefaultRetention defaultRetention) {
        this.isFileLockEnabled = isFileLockEnabled;
        this.defaultRetention = defaultRetention;
    }

    public B2BucketFileLockConfiguration(boolean isFileLockEnabled,
                                         String mode,
                                         int duration,
                                         String unit) {
        this.isFileLockEnabled = isFileLockEnabled;
        this.defaultRetention = new B2BucketDefaultRetention(mode, new B2BucketDefaultRetentionPeriod(duration, unit));
    }

    public B2BucketFileLockConfiguration(boolean isFileLockEnabled) {
        this.isFileLockEnabled = isFileLockEnabled;
        this.defaultRetention = new B2BucketDefaultRetention(null, null);
    }

    public boolean isFileLockEnabled() {
        return isFileLockEnabled;
    }

    public B2BucketDefaultRetention getDefaultRetention() {
        return defaultRetention;
    }

    public String getMode() {
        if (defaultRetention == null) {
            return null;
        }
        return defaultRetention.getMode();
    }

    public B2BucketDefaultRetentionPeriod getPeriod() {
        if (defaultRetention == null) {
            return null;
        }
        return defaultRetention.getPeriod();
    }

    @Override
    public String toString() {
        return (isFileLockEnabled + "," + defaultRetention);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2BucketFileLockConfiguration that = (B2BucketFileLockConfiguration) o;
        return isFileLockEnabled == that.isFileLockEnabled &&
                Objects.equals(defaultRetention, that.defaultRetention);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isFileLockEnabled, defaultRetention);
    }

}