/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.structures;

import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.util.B2Preconditions;
import com.backblaze.b2.util.B2StringUtil;

import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Custom headers for B2WebhookConfigurationForRequest
 */
public class B2CustomHeaderForRequest implements Comparable<B2CustomHeaderForRequest> {

    private static final Comparator<B2CustomHeaderForRequest> COMPARATOR =
            Comparator.comparing(B2CustomHeaderForRequest::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(B2CustomHeaderForRequest::getValue);

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
    public B2CustomHeaderForRequest(String name,
                                    String value) {

        B2Preconditions.checkArgument(!B2StringUtil.isEmpty(name), "the name must not be empty");

        this.name = name;
        this.value = value;
    }

    static TreeSet<B2CustomHeaderForRequest> convertToTreeSetOfB2CustomHeaderForRequest(
            TreeSet<B2CustomHeaderForResponse> customHeaderForResponseSetOrNull) {
        if (customHeaderForResponseSetOrNull == null) {
            return null;
        }

        final TreeSet<B2CustomHeaderForRequest> customHeaderForRequestSet = new TreeSet<>();
        for (B2CustomHeaderForResponse b2CustomHeaderForResponse : customHeaderForResponseSetOrNull) {
            customHeaderForRequestSet.add(
                    new B2CustomHeaderForRequest(
                            b2CustomHeaderForResponse.getName(), b2CustomHeaderForResponse.getValue()
                    )
            );
        }
        return customHeaderForRequestSet;
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
        final B2CustomHeaderForRequest that = (B2CustomHeaderForRequest) o;
        return name.equals(that.name) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return "B2CustomHeaderForRequest{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    /**
     * B2CustomHeaders are sorted (without case sensitivity) by name first, and then value.
     */
    @Override
    public int compareTo(B2CustomHeaderForRequest c) {
        return COMPARATOR.compare(this, c);
    }
}
