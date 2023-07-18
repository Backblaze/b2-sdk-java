/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2Collections;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Set;

class B2JsonDeserializationUtil {

    /**
     * Returns the object constructor that is annotated with the {@code B2Json} annotation.
     * @throws B2JsonException if there is not a {@code B2Json} annotated constructor found, or
     *                         multiple constructors found with the {@code B2Json} annotation.
     */
    static <T> Constructor<T>  findB2JsonConstructor(Class<T> clazz) throws B2JsonException {
        // Find the constructor to use.
        Constructor<T> chosenConstructor = null;
        for (Constructor<?> candidate : clazz.getDeclaredConstructors()) {
            if (candidate.getAnnotation(B2Json.constructor.class) != null) {
                if (chosenConstructor != null) {
                    throw new B2JsonException(clazz.getName() + " has two constructors selected");
                }
                //noinspection unchecked
                chosenConstructor = (Constructor<T>) candidate;
                chosenConstructor.setAccessible(true);
            }
        }
        if (chosenConstructor == null) {
            throw new B2JsonException(clazz.getName() + " has no constructor annotated with B2Json.constructor");
        }
        return chosenConstructor;
    }

    /**
     * Returns the set of discarded fields that are configured on the {@code B2Json} annotated constructor. If there
     * are no discards set, an empty set is returned.
     */
    static <T> Set<String> getDiscards(Constructor<T> b2JsonConstructor) {
        final B2Json.constructor annotation = b2JsonConstructor.getAnnotation(B2Json.constructor.class);

        if (annotation == null) {
            return Collections.emptySet();
        }

        String discardsWithCommas = annotation.discards().replace(" ", "");
        if (discardsWithCommas.isEmpty()) {
            return Collections.emptySet();
        }

        String[] discardNames = discardsWithCommas.split(",");
        return B2Collections.unmodifiableSet(discardNames);
    }
}
