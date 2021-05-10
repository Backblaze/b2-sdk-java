/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2BucketDefaultRetention {

    /**
     * The default file retention mode, i.e. "governance" or "compliance" (or null for no retention)
     */
    @B2Json.optional
    private final String mode;

    /**
     * The default file retention period (or null for no retention)
     */
    @B2Json.optional
    private final B2BucketDefaultRetentionPeriod period;

    @B2Json.constructor(params = "mode, period")
    public B2BucketDefaultRetention(String mode, B2BucketDefaultRetentionPeriod period) {
        this.mode = mode;
        this.period = period;
    }

    public B2BucketDefaultRetention(String mode, int duration, String unit) {
        this.mode = mode;
        this.period = new B2BucketDefaultRetentionPeriod(duration, unit);
    }

    public String getMode() { return mode; }

    public B2BucketDefaultRetentionPeriod getPeriod() { return period; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2BucketDefaultRetention that = (B2BucketDefaultRetention) o;
        return Objects.equals(mode, that.getMode()) &&
                Objects.equals(period, that.getPeriod());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, period);
    }

    @Override
    public String toString() {
        return "B2FileRetention{" +
                "mode=" + mode + ", " +
                "period=" + period +
                '}';
    }
}
