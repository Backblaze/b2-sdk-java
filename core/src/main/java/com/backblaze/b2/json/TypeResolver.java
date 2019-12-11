/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class TypeResolver {

    private final Class<?> clazz;
    private final Type type;
    private final Map<String, Type> typeMap;

    public TypeResolver(Class<?> clazz) {
        this(clazz, null);
    }

    public TypeResolver(Class<?> clazz, Type[] actualTypeArguments) {
        this.clazz = clazz;

        // If there are no type arguments, then this is just a concrete class.
        if (actualTypeArguments == null) {
            this.type = clazz;
            this.typeMap = null;
        } else {
            TypeVariable[] typeParameters = clazz.getTypeParameters();
            if (typeParameters.length != actualTypeArguments.length) {
                throw new RuntimeException("actualTypeArguments must be same length as class' type parameters");
            }

            this.type = new ResolvedParameterizedType(clazz, actualTypeArguments);
            this.typeMap = new TreeMap<>();
            for (int i = 0; i < typeParameters.length; i++) {
                typeMap.put(typeParameters[i].getName(), actualTypeArguments[i]);
            }
        }
    }

    public Field[] getDeclaredFields() {
        return clazz.getDeclaredFields();
    }

    public Type getType() {
        return type;
    }

    public Type resolveType(Field field) {
        return resolveType(field.getGenericType());
    }

    public Type resolveType(Type type) {
        if (type instanceof Class) {
            return type;
        }


        if (type instanceof TypeVariable) {
            // If we're here, then type needs to be resolved.
            // If there's no typeMap, then throw, because we cannot resolve anything.
            if (typeMap == null) {
                throw new RuntimeException("Cannot resolve type " + type + " - the typeMap is empty");
            }
            return typeMap.get(type.getTypeName());
        }
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType)type;
            final Type[] resolvedActualTypeArguments = resolveTypes(parameterizedType.getActualTypeArguments());
            // TODO can we safely assume rawType will always be a class?
            return new ResolvedParameterizedType(parameterizedType.getRawType(), resolvedActualTypeArguments);
        }
        if (type instanceof GenericArrayType) {
            final GenericArrayType genericArrayType = (GenericArrayType)type;
            final Type resolvedComponentType = resolveType(genericArrayType.getGenericComponentType());
            return new ResolvedGenericArrayType(resolvedComponentType);
        }
        if (type instanceof WildcardType) {
            throw new RuntimeException("Wildcard types are not supported");
        }
        throw new RuntimeException("Do not know how to resolve type " + type.getClass());
    }

    private Type[] resolveTypes(Type[] types) {
        final Type[] resolvedTypes = new Type[types.length];

        for (int i = 0; i < types.length; i++) {
            resolvedTypes[i] = resolveType(types[i]);
        }
        return resolvedTypes;
    }

    static class ResolvedGenericArrayType implements GenericArrayType {

        private Type genericComponentType;

        public ResolvedGenericArrayType(Type genericComponentType) {
            this.genericComponentType = genericComponentType;
        }

        @Override
        public Type getGenericComponentType() {
            return genericComponentType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResolvedGenericArrayType that = (ResolvedGenericArrayType) o;
            return Objects.equals(genericComponentType, that.genericComponentType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(genericComponentType);
        }
    }

    static class ResolvedParameterizedType implements ParameterizedType {

        private final Type rawType;
        private final Type[] actualTypeArguments;

        public ResolvedParameterizedType(Type rawType, Type[] actualTypeArguments) {
            this.rawType = rawType;
            this.actualTypeArguments = actualTypeArguments;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            // TODO implement (if needed)?
            throw new RuntimeException("this shouldn't be called");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResolvedParameterizedType that = (ResolvedParameterizedType) o;
            return Objects.equals(rawType, that.rawType) &&
                    Arrays.equals(actualTypeArguments, that.actualTypeArguments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rawType, actualTypeArguments);
        }
    }
}
