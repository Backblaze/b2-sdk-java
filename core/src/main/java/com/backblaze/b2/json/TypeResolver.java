/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import com.backblaze.b2.util.B2Preconditions;

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

/**
 * Class used to resolve generic types of a class when supplied with the actual type arguments used to construct an
 * instance of the class.
 *
 * For example, consider the following classes.
 *
 * class Item<T> {
 *     T value;
 *     List<T> values;
 * }
 *
 * class Enclosing {
 *     Item<String> stringItem;
 *     Item<Integer> integerItem;
 * }
 *
 * When considering just Item.class, we do not have enough information to resolve the type for .value and .values.
 * However, if we are considering Enclosing.class, then even though it has fields that are Item.class instances, we have
 * the additional context of the type arguments used along with the Item class. This allows us to resolve the types of
 * Enclosing.class' fields.
 *
 * final TypeResolver typeResolver = new TypeResolver(Enclosing.class);
 * // The following returns ResolvedParameterizedType(Item.class, new Type[]{ String.class });
 * typeResolver.resolveType(Enclosing.class.getDeclaredFields()[0]);
 *
 */
public class TypeResolver {

    private final Class<?> clazz;
    private final Type type;
    private final Map<String, Type> typeMap;

    public TypeResolver(Class<?> clazz) {
        this(clazz, null);
    }

    public TypeResolver(Class<?> clazz, Type[] actualTypeArgumentsOrNull) {
        B2Preconditions.checkArgumentIsNotNull(clazz, "Supplied class must not be null");
        TypeVariable[] typeParameters = clazz.getTypeParameters();
        if (typeParameters.length == 0) {
            B2Preconditions.checkArgument(
                    actualTypeArgumentsOrNull == null || actualTypeArgumentsOrNull.length == 0,
                    "Cannot create TypeResolver with type arguments. Class " + clazz.getName() + " has no type parameters");
        } else {
            B2Preconditions.checkArgument(
                    actualTypeArgumentsOrNull != null && typeParameters.length == actualTypeArgumentsOrNull.length,
                    "actualTypeArguments must be same length as class' type parameters");
        }

        this.clazz = clazz;

        // If there are no type arguments, then this is just a concrete class.
        if (typeParameters.length == 0) {
            this.type = clazz;
            this.typeMap = null;
        } else {
            this.type = new ResolvedParameterizedType(clazz, actualTypeArgumentsOrNull);
            this.typeMap = new TreeMap<>();
            for (int i = 0; i < typeParameters.length; i++) {
                typeMap.put(typeParameters[i].getName(), actualTypeArgumentsOrNull[i]);
            }
        }
    }

    public Field[] getDeclaredFields() {
        return clazz.getDeclaredFields();
    }

    public Type getType() {
        return type;
    }

    /**
     * Resolve the type of the supplied field.
     *
     * Will throw if field does not belong to the class this TypeResolver is for.
     */
    public Type resolveType(Field field) {
        B2Preconditions.checkArgument(
                field.getDeclaringClass().equals(this.clazz),
                "cannot resolve fields from other classes");
        return resolveType(field.getGenericType());
    }

    private Type resolveType(Type type) {
        if (type instanceof Class) {
            return type;
        }

        if (type instanceof TypeVariable) {
            // If we're here, then type needs to be resolved.
            // If there's no typeMap, then throw, because we cannot resolve anything.
            final String typeName = type.getTypeName();
            B2Preconditions.checkState(typeMap.containsKey(typeName));
            return typeMap.get(typeName);
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

    /**
     * Class that represents a generic array.
     *
     * class Outer<T> {
     *     T[] genericArray;
     * }
     */
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

    /**
     * Class that represents a parameterized type.
     *
     * class Outer<T> {
     *     Item<T> parameterizedType1;
     *     Item<String> parameterizedType2;
     * }
     */
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
            int result = Objects.hash(rawType);
            result = 31 * result + Arrays.hashCode(actualTypeArguments);
            return result;
        }
    }
}
