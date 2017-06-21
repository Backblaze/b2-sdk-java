/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.List;
import java.util.Objects;

public class B2ListPartsResponse {
    @B2Json.required
    private final List<B2Part> parts;

    @B2Json.optional
    private final Integer nextPartNumber;

    @B2Json.constructor(params = "parts,nextPartNumber")
    public B2ListPartsResponse(List<B2Part> parts,
                               Integer nextPartNumber) {
        this.parts = parts;
        this.nextPartNumber = nextPartNumber;
    }

    public List<B2Part> getParts() {
        return parts;
    }

    public Integer getNextPartNumber() {
        return nextPartNumber;
    }

    /**
     * @return true iff this is the last set of responses.
     */
    public boolean atEnd() {
        return nextPartNumber == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2ListPartsResponse that = (B2ListPartsResponse) o;
        return Objects.equals(getParts(), that.getParts()) &&
                Objects.equals(getNextPartNumber(), that.getNextPartNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParts(), getNextPartNumber());
    }
}
