/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class B2CollectionsTest extends B2BaseTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testUnmodifiableSet() {
        final Set<String> set = B2Collections.unmodifiableSet(new String[] {"a", "b"});
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));

        thrown.expect(UnsupportedOperationException.class);
        set.add("mutation");
    }

    @Test
    public void testMapOf() {
        final Map<String, Integer> empty = B2Collections.mapOf();
        assertEquals(0, empty.size());

        final Map<String, Integer> one = B2Collections.mapOf("a", 1);
        assertEquals(1, one.size());
        assertEquals((Integer) 1, one.get("a"));


        final Map<String, Integer> two = B2Collections.mapOf(
                "a", 1,
                "b", 2);
        assertEquals(2, two.size());
        assertEquals((Integer) 1, two.get("a"));
        assertEquals((Integer) 2, two.get("b"));

        final Map<String, Integer> three = B2Collections.mapOf(
                "a", 1,
                "b", 2,
                "c", 3);
        assertEquals(3, three.size());
        assertEquals((Integer) 1, three.get("a"));
        assertEquals((Integer) 2, three.get("b"));
        assertEquals((Integer) 3, three.get("c"));
    }

    @Test
    public void testUnmodifiableMap() {
        final Map<String,Integer> one = B2Collections.unmodifiableMap(B2Collections.mapOf("a", 1));

        thrown.expect(UnsupportedOperationException.class);
        one.put("mutation", 666);
    }

    @Test
    public void testListOf() {
        final List<String> empty = B2Collections.listOf();
        assertTrue(empty.isEmpty());

        final List<String> three = B2Collections.listOf("a", "b", "c");
        assertEquals(3, three.size());
        assertEquals("a", three.get(0));
        assertEquals("b", three.get(1));
        assertEquals("c", three.get(2));
    }


    @Test
    public void test_forCoverage() {
        new B2Collections();
    }
}
