/*
 * Copyright 2020, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Objects;

public class B2BucketFileLockPeriod {

    @B2Json.required
    private final int duration;

    @B2Json.required
    private final String unit;

    @B2Json.constructor(params = "duration, unit")
    public B2BucketFileLockPeriod(int duration, String unit) {
        this.duration = duration;
        this.unit = unit;
    }

    public int getDuration() { return duration; }

    public String getUnit() { return unit; }

    @Override
    public String toString() {
        return duration + "," + (unit == null ? "null" : unit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        B2BucketFileLockPeriod period = (B2BucketFileLockPeriod) o;
        return this.duration == period.duration &&
                Objects.equals(this.unit, period.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration, unit);
    }
}