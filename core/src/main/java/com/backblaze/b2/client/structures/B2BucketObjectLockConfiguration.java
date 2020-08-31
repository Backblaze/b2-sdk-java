/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2BucketObjectLockConfiguration {

    @B2Json.required
    private final String status;

    @B2Json.optional
    private final B2BucketObjectLockPeriod period;

    @B2Json.optional
    private final String mode;

    @B2Json.constructor(params = "status, period, mode")
    public B2BucketObjectLockConfiguration(String status,
                                           B2BucketObjectLockPeriod period,
                                           String mode) {
        this.status = status;
        this.period = period;
        this.mode = mode;
    }

    public B2BucketObjectLockConfiguration(String status,
                                           int duration,
                                           String unit,
                                           String mode) {
        this.status = status;
        this.period = new B2BucketObjectLockPeriod(duration, unit);
        this.mode = mode;
    }

    public B2BucketObjectLockConfiguration(boolean isObjectLockEnabled) {
        this.status = isObjectLockEnabled ? B2ObjectLockStatus.ENABLED : B2ObjectLockStatus.DISABLED;
        this.period = null;
        this.mode = null;
    }

    public String getStatus() { return status; }

    public B2BucketObjectLockPeriod getPeriod() { return period; }

    public String getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return (status == null ? "null" : status) + "," +
                (period == null ? "null" : period) + "," +
                (mode == null ? "null" : mode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2BucketObjectLockConfiguration lockConfiguration = (B2BucketObjectLockConfiguration) o;
        return Objects.equals(this.status, lockConfiguration.status) &&
                Objects.equals(this.mode, lockConfiguration.mode) &&
                Objects.equals(this.period, lockConfiguration.period);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, period, mode);
    }
}