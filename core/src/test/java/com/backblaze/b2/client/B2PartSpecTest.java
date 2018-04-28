/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.util.B2BaseTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class B2PartSpecTest extends B2BaseTest {
    @Test
    public void test() {
        final B2PartSpec a = new B2PartSpec(1, 0, 100);
        assertEquals("B2PartSpec{#1, start=0, pastEnd=100}", a.toString());
        assertEquals(a, a);

        final B2PartSpec b = new B2PartSpec(1, 0, 100);
        assertEquals(a, b);

        final B2PartSpec c = new B2PartSpec(2, 0, 100);
        assertNotEquals(a, c);

        // for coverage
        //noinspection ResultOfMethodCallIgnored
        a.hashCode();
    }

    @Test
    public void testCompareTo() {
        final B2PartSpec a1 = new B2PartSpec(1, 0, 100);
        final B2PartSpec a2 = new B2PartSpec(1, 0, 100);
        assertEquals(0, a1.compareTo(a2));

        // b comes after a1 because it has a higher partNumber
        final B2PartSpec b = new B2PartSpec(2, 0, 100);
        assertTrue(b.compareTo(a1) > 0);

        // c comes after a1 because it has a higher start.
        final B2PartSpec c = new B2PartSpec(1, 1, 100);
        assertTrue(c.compareTo(a1) > 0);

        // d comes after a1 because it has a higher length
        final B2PartSpec d = new B2PartSpec(1, 0, 101);
        assertTrue(d.compareTo(a1) > 0);
    }
}
