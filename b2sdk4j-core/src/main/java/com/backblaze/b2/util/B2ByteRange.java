/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.util;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a range of bytes in an HTTP entity.
 *
 * Used both for the Range header and the Content-Range header.
 *
 * In the Range header, we do not support multiple ranges, but do support
 * all three forms of single ranges:
 *     bytes=10-20     11 bytes, from index 10 to index 20
 *     bytes=15-       All bytes from index 15 to the end of the entity.
 *     bytes=-37       The last 37 bytes of the entity.
 *
 * See:
 *     http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.16
 *     http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35
 */
public class B2ByteRange {

    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=(\\d*)-(\\d*)");

    /**
     * The index of the first byte in the range, zero based,
     * or null if there is no starting index.
     */
    public final Long start;

    /**
     * The index of the last byte in the range, inclusive.
     * The range "bytes=0-0" is a one-byte range that is the
     * first byte in the content.
     * Null if there is no ending index.
     */
    public final Long end;

    public static B2ByteRange startAt(long start) {
        return new B2ByteRange(start, null);
    }
    public static B2ByteRange between(long start, long end) {
        return new B2ByteRange(start, end);
    }


    /**
     * Parses a byte range that looks like:
     *
     *     bytes=100-999
     *
     * Returns null on error.
     */
    public static B2ByteRange parse(String s) {
        if (s == null) {
            return null;
        }
        Matcher matcher = RANGE_PATTERN.matcher(s);
        if (!matcher.matches()) {
            return null;
        }

        Long start = longOrNullFromStr(matcher.group(1));
        Long end = longOrNullFromStr(matcher.group(2));
        if (start == null && end == null) {
            return null;
        }
        if (start != null && end != null && end < start) {
            return null;
        }
        return new B2ByteRange(start, end);
    }

    /**
     * Initializes a new B2ByteRange.
     *
     * Remember that "end" is the index of the last byte in the range,
     * inclusive.
     */
    public B2ByteRange(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Can this range be satisfied with a file of the given length.
     */
    public boolean isSatisfied(long entitySize) {
        if (start == null) {
            return end != 0;
        }
        else {
            return start < entitySize;
        }
    }

    /**
     * Return an equivalent byte range with both start and end set, and
     * with both being valid indexes into the entity.
     *
     * Caller is responsible for making sure the range is satisfiable with the
     * given entity size.
     */
    public B2ByteRange effectiveRange(long entitySize) {
        B2Preconditions.checkArgument(isSatisfied(entitySize));
        if (start == null) {
            return new B2ByteRange(Math.max(0L, entitySize - end), entitySize - 1);
        }
        else if (end == null) {
            return new B2ByteRange(start, entitySize - 1);
        }
        else {
            return new B2ByteRange(start, Math.min(end, entitySize - 1));
        }
    }

    /**
     * Returns the number of bytes that the range specifies.
     */
    public long getNumberOfBytes() {
        B2Preconditions.checkState(start != null && end != null);
        return end - start + 1;
    }

    /**
     * Returns a string like "bytes=200-340"
     */
    @Override
    public String toString() {
        return "bytes=" + strFromLongOrNull(start) + "-" +  strFromLongOrNull(end);
    }

    /**
     * Converts a string to a Long, or to null if the
     * string is empty.
     */
    private static Long longOrNullFromStr(String s) {
        if (s.length() == 0) {
            return null;
        }
        else {
            return Long.valueOf(s);
        }
    }

    /**
     * Returns "" for null, or the value of the long.
     */
    private static String strFromLongOrNull(Long value) {
        if (value == null) {
            return "";
        }
        else {
            return value.toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        B2ByteRange that = (B2ByteRange) o;
        return Objects.equals(start, that.start) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}
