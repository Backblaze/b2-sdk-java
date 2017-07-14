/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A B2BoundedLruMap is a hash map that keeps up to a fixed maximum number of entries.
 *
 * NOTE: this is a subclass of LinkedHashMap (instead of containing the map)
 *       because we need override removeEldestEntry.
 *
 * THREAD-SAFETY: This class is NOT thread-safe by itself.
 */
class B2BoundedLruMap<K,V> extends LinkedHashMap<K,V> {
    private int maxEntries = 20;

    /**
     * @return a new LRU (by access order) map with the given maximum
     * number of entries.
     */
    @SuppressWarnings("SameParameterValue")
    static <K,V> B2BoundedLruMap<K,V> withMax(int maxEntries) {
        B2BoundedLruMap<K,V> map = new B2BoundedLruMap<>();
        map.setMaxEntries(maxEntries);
        return map;
    }

    private B2BoundedLruMap() {
        // uses default values for cap & loadFactor, as shown in javadoc.
        super(16, 0.75f, true /*useAccessOrder*/);
    }

    private void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > maxEntries;
    }
}
