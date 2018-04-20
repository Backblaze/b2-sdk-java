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
         * @param typeName The name used for this type in JSON.
         * @param typeClass The class used in Java.
         * @throws B2JsonException When adding duplicate names or classes to the map.
         */
        public Builder put(String typeName, Class<?> typeClass) throws B2JsonException {
            if (typeNameToClass.containsKey(typeName)) {
                throw new B2JsonException("duplicate type name in union type map: '" + typeName + "'");
            }
            if (classToTypeName.containsKey(typeClass)) {
                throw new B2JsonException("duplicate class in union type map: " + typeClass);
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
