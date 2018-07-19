/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Objects;

public class B2Allowed {

    @B2Json.required
    public final List<String> capabilities;

    @B2Json.optional
    public final String bucketId;

    @B2Json.optional
    public final String namePrefix;

    public List<String> getCapabilities() {
        return capabilities;
    }

    public String getBucketId() {
        return bucketId;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    @B2Json.constructor(params = "capabilities, bucketId, namePrefix")
    public B2Allowed(List<String> capabilities, String bucketId, String namePrefix) {
        this.capabilities = capabilities;
        this.bucketId = bucketId;
        this.namePrefix = namePrefix;
    }

    @Override
    public String toString() {
        return "B2Allowed(" +
                "capabilities=" + capabilities +
                ", bucketId=" + bucketId +
                ", namePrefix=" + namePrefix +
                ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2Allowed b2Allowed = (B2Allowed) o;
        return Objects.equals(getCapabilities(), b2Allowed.getCapabilities()) &&
                Objects.equals(getBucketId(), b2Allowed.getBucketId()) &&
                Objects.equals(getNamePrefix(), b2Allowed.getNamePrefix());
    }
}

