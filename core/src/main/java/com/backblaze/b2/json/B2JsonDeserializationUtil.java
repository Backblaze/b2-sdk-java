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
    static <T> Constructor<T> findConstructor(Class<T> clazz) throws B2JsonException {
        // Find the constructor to use.
        final Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
        if (declaredConstructors.length == 0) {
            throw new B2JsonException(clazz.getName() + " has no constructor");
        }

        Constructor<T> chosenConstructor = null;
        for (Constructor<?> candidate : declaredConstructors) {
            if (candidate.getAnnotation(B2Json.constructor.class) != null) {
                if (chosenConstructor != null) {
                    throw new B2JsonException(clazz.getName() + " has two constructors selected");
                }
                //noinspection unchecked
                chosenConstructor = (Constructor<T>) candidate;
            }
        }

        B2Json.type b2JsonTypeAnnotation = clazz.getAnnotation(B2Json.type.class);
        if (chosenConstructor == null) {
            // ensure there is only one constructor. If the user has multiple constructors, then one should have
            // a @B2Json.constructor annotation.
            if (declaredConstructors.length > 1) {
                throw new B2JsonException(clazz.getName() + " has multiple constructors without @B2Json.constructor");
            }
            // verify that class has the @B2Json.type annotation
            if (b2JsonTypeAnnotation != null) {
                //noinspection unchecked
                chosenConstructor = (Constructor<T>) declaredConstructors[0];
            }
        } else {
            // verify the class doesn't have both @B2Json.constructor and @B2Json.type annotations
            if (b2JsonTypeAnnotation != null) {
                throw new B2JsonException(clazz.getName() + " has both @B2Json.type and @B2Json.constructor annotations");
            }
        }
        
        if (chosenConstructor == null) {
            throw new B2JsonException(clazz.getName() + " has no constructor annotated with B2Json.constructor");
        }
        chosenConstructor.setAccessible(true);
        return chosenConstructor;
    }

    /**
     * Returns the set of discarded fields that are configured on the {@code B2Json} annotated constructor or class.
     * If there are no discards set, an empty set is returned.
     */
    static <T> Set<String> getDiscards(B2Json.B2JsonTypeConfig params) {
        final String discardsWithCommas = params.discards.replace(" ", "");
        if (discardsWithCommas.isEmpty()) {
            return Collections.emptySet();
        }

        String[] discardNames = discardsWithCommas.split(",");
        return B2Collections.unmodifiableSet(discardNames);
    }
}
