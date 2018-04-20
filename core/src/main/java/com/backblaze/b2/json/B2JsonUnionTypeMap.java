/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the mapping between type names and the classes used for them in a union type.
 */
public class B2JsonUnionTypeMap {

    private final Map<String, Class<?>> typeNameToClass;
    private final Map<Class<?>, String> classToTypeName;

    private B2JsonUnionTypeMap(Map<String, Class<?>> typeNameToClass, Map<Class<?>, String> classToTypeName) {
        this.typeNameToClass = typeNameToClass;
        this.classToTypeName = classToTypeName;
    }

    public Map<String, Class<?>> getTypeNameToClass() {
        return typeNameToClass;
    }

    public String getTypeNameOrNullForClass(Class<?> clazz) {
        return classToTypeName.get(clazz);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<String, Class<?>> typeNameToClass = new HashMap<>();
        private final Map<Class<?>, String> classToTypeName = new HashMap<>();

        private Builder() {}

        /**
         * Adds a new type to the map being built.
         *
         * This can't throw a B2JsonException, because these builders are used in
         * static initializers, where it would be really awkward.  The B2Json code
         * that calls the getUnionTypeMap() method will translate the RuntimeException
         * thrown into a B2JsonException.
         *
         * @param typeName The name used for this type in JSON.
         * @param typeClass The class used in Java.
         */
        public Builder put(String typeName, Class<?> typeClass) {
            if (typeNameToClass.containsKey(typeName)) {
                throw new RuntimeException("duplicate type name in union type map: '" + typeName + "'");
            }
            if (classToTypeName.containsKey(typeClass)) {
                throw new RuntimeException("duplicate class in union type map: " + typeClass);
            }
            typeNameToClass.put(typeName, typeClass);
            classToTypeName.put(typeClass, typeName);
            return this;
        }

        /**
         * Builds the B2JsonUnionTypeMap.
         */
        public B2JsonUnionTypeMap build() {
            return new B2JsonUnionTypeMap(
                    Collections.unmodifiableMap(typeNameToClass),
                    Collections.unmodifiableMap(classToTypeName)
            );
        }
    }

}
