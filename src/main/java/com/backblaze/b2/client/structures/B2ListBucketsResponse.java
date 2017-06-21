/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Objects;

public class B2ListBucketsResponse {
    @B2Json.required
    private final List<B2Bucket> buckets;

    @B2Json.constructor(params = "buckets")
    public B2ListBucketsResponse(List<B2Bucket> buckets) {
        this.buckets = buckets;
    }

    public List<B2Bucket> getBuckets() {
        return buckets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2ListBucketsResponse that = (B2ListBucketsResponse) o;
        return Objects.equals(getBuckets(), that.getBuckets());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBuckets());
    }
}
