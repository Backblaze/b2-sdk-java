/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class B2BoundedLruMapTest {
    @Test
    public void testOrderingAndMax() {
        final B2BoundedLruMap<String,Integer> map = B2BoundedLruMap.withMax(3);

        assertEquals(0, map.size());

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        checkContents(map, "a", "b", "c");

        // make sure we don't go past the bounds & that we get rid of the oldest accessed.
        map.put("d", 4);
        checkContents(map, "b", "c", "d");

        // access "b" to make it not the oldest.
        map.get("b");
        checkContents(map, "c", "d", "b");

        // add another to see what gets ditched
        map.put("e", 5);
        checkContents(map, "d", "b", "e");
    }

    private void checkContents(B2BoundedLruMap<String, Integer> map, String... expectedNames) {
        final String[] actualNames = map.keySet().toArray(new String[map.size()]);

        assertArrayEquals(expectedNames, actualNames);
    }
}
