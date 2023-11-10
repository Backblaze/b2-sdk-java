/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;

import java.util.Comparator;
import java.util.Objects;

/**
 * Custom headers for B2WebhookConfigurationForResponse
 */
public class B2CustomHeaderForResponse implements Comparable<B2CustomHeaderForResponse> {

    private static final Comparator<B2CustomHeaderForResponse> COMPARATOR =
            Comparator.comparing(B2CustomHeaderForResponse::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(B2CustomHeaderForResponse::getValue);

    /**
     * The name of the custom header.  Must never be "".
     */
    @B2Json.required
    private final String name;

    /**
     * The value of the custom header
     */
    @B2Json.required
    private final String value;

    @B2Json.constructor
    public B2CustomHeaderForResponse(String name,
                                     String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final B2CustomHeaderForResponse that = (B2CustomHeaderForResponse) o;
        return name.equals(that.name) &&
                value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return "B2CustomHeaderForResponse{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    /**
     * B2CustomHeaders are sorted (without case sensitivity) by name first, and then value.
     */
    @Override
    public int compareTo(B2CustomHeaderForResponse c) {
        return COMPARATOR.compare(this, c);
    }
}
