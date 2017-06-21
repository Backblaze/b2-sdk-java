/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class B2Collections {
    public static <T> Set<T> unmodifiableSet(T[] elements) {
        final Set<T> set = new HashSet<>(elements.length);
        set.addAll(Arrays.asList(elements));
        return Collections.unmodifiableSet(set);
    }

    public static <K,V> Map<K,V> unmodifiableMap(Map<K,V> orig) {
        final Map<K,V> map = new TreeMap<>();
        map.putAll(orig);
        return Collections.unmodifiableMap(map);
    }

    public static <K,V> Map<K, V> mapOf() {
        return new TreeMap<>();
    }

    public static <K,V> Map<K, V> mapOf(K k1, V v1) {
        final Map<K,V> map = new TreeMap<>();
        map.put(k1, v1);
        return map;
    }
    public static <K,V> Map<K, V> mapOf(K k1, V v1,
                                        K k2, V v2) {
        final Map<K,V> map = new TreeMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
    public static <K,V> Map<K, V> mapOf(K k1, V v1,
                                        K k2, V v2,
                                        K k3, V v3) {
        final Map<K,V> map = new TreeMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }



    @SafeVarargs
    public static <T> List<T> listOf(T... orig) {
        return Arrays.asList(orig);
    }

}
