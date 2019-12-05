/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.TreeMap;

public class TypeResolver {

    private final Class<?> clazz;
    private final Map<String, Type> typeMap;

    public TypeResolver(Class<?> clazz) {
        this(clazz, null);
    }

    public TypeResolver(Class<?> clazz, Type[] actualTypeParameters) {
        this.clazz = clazz;
        this.typeMap = buildTypeMap(clazz, actualTypeParameters);
    }

    private static Map<String, Type> buildTypeMap(Class<?> clazz, Type[] actualTypeParameters) {
        if (actualTypeParameters == null) {
            return null;
        }

        TypeVariable[] typeParameters = clazz.getTypeParameters();
        if (typeParameters.length != actualTypeParameters.length) {
            throw new RuntimeException("typeParameters/actualTypeParameters mismatch");
        }

        Map<String, Type> typeMap = new TreeMap<>();
        for (int i = 0; i < typeParameters.length; i++) {
            typeMap.put(typeParameters[i].getName(), actualTypeParameters[i]);
        }
        return typeMap;
    }

    public Field[] getDeclaredFields() {
        return clazz.getDeclaredFields();
    }

    public Type resolveType(Field field) {
        return resolveType(field.getGenericType());
    }

    public Type resolveType(Type type) {
        if (type instanceof Class) {
            return type;
        }
        if (type instanceof TypeVariable) {
            return typeMap.get(type.getTypeName());
        }
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType)type;
            parameterizedType.getRawType();
            final Type[] resolvedActualTypeArguments = resolveTypes(parameterizedType.getActualTypeArguments());
            return new ResolvedParameterizedType(parameterizedType.getRawType(), resolvedActualTypeArguments);
        }
        throw new RuntimeException("Could not resolve type " + type);
    }

    private Type[] resolveTypes(Type[] types) {
        final Type[] resolvedTypes = new Type[types.length];

        for (int i = 0; i < types.length; i++) {
            resolvedTypes[i] = resolveType(types[i]);
        }
        return resolvedTypes;
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
            // TODO
            return null;
        }
    }
}
