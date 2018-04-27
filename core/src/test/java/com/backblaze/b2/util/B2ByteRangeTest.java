/**
 * Unit tests for B2ByteRange
 *
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class B2ByteRangeTest extends B2BaseTest {

    @Test
    public void testStartAt() {
        final B2ByteRange r = B2ByteRange.startAt(1234);
        assertEquals((Long) 1234L, r.start);
        assertNull(r.end);
        assertEquals("bytes=1234-", r.toString());
    }

    @Test
    public void testBetween() {
        final B2ByteRange r = B2ByteRange.between(4321,9876);
        assertEquals((Long) 4321L, r.start);
        assertEquals((Long) 9876L, r.end);
        assertEquals("bytes=4321-9876", r.toString());
    }

    @Test
    public void testSimple() {
        checkBothWays(new B2ByteRange(3L, 4L), "bytes=3-4");
    }

    @Test
    public void testNoStart() {
        checkBothWays(new B2ByteRange(null, 4L), "bytes=-4");
    }

    @Test
    public void testNoEnd() {
        checkBothWays(new B2ByteRange(3L, null), "bytes=3-");
    }

    private void checkBothWays(B2ByteRange range, String formatted) {
        assertEquals(formatted, range.toString());
        assertEquals(range, B2ByteRange.parse(formatted));
    }

    @Test
    public void testGarbage() {
        assertNull(B2ByteRange.parse(null));
        assertNull(B2ByteRange.parse("garbage"));
        assertNull(B2ByteRange.parse("bytes=-"));
        assertNull(B2ByteRange.parse("bytes=5-2"));
    }

    @Test
    public void testNumberOfBytes() {
        //noinspection ConstantConditions
        assertEquals(20, B2ByteRange.parse("bytes=0-19").getNumberOfBytes());
    }

    @Test
    public void testSatisfied() {
        checkSatisfied(true, "bytes=3-", 4);
        checkSatisfied(false, "bytes=3-", 3);
        checkSatisfied(true, "bytes=3-9", 4);
        checkSatisfied(false, "bytes=3-9", 3);
        checkSatisfied(false, "bytes=-0", 5);  // spec isn't clear on this, but this is what Amazon does
        checkSatisfied(true, "bytes=-1", 5);
        checkSatisfied(true, "bytes=-100", 5);
    }

    private void checkSatisfied(boolean expected, String formatted, long entitySize) {
        B2ByteRange parsed = B2ByteRange.parse(formatted);
        assertNotNull(parsed);
        assertEquals(expected, parsed.isSatisfied(entitySize));

    }

    @Test
    public void testEffectiveBytRange() {
        checkEffective("bytes=0-15", "bytes=0-15", 20);
        checkEffective("bytes=10-15", "bytes=10-15", 20);
        checkEffective("bytes=10-19", "bytes=10-30", 20);
        checkEffective("bytes=10-19", "bytes=10-", 20);
        checkEffective("bytes=0-19", "bytes=-30", 20);
        checkEffective("bytes=13-19", "bytes=-7", 20);
        checkEffective("bytes=19-19", "bytes=-1", 20);
    }

    public void checkEffective(String expected, String spec, long entitySize) {
        //noinspection ConstantConditions
        assertEquals(expected, B2ByteRange.parse(spec).effectiveRange(entitySize).toString());
    }

}
