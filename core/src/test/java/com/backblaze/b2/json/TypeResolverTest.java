/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TypeResolverTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final TypeResolver resolverWithoutActualTypes = new TypeResolver(TestClass.class);
    private final TypeResolver resolverWithActualTypes = new TypeResolver(
            TestClass.class,
            new Type[]{String.class, Integer.class});
    private final Field[] declaredFields = TestClass.class.getDeclaredFields();


    @Test
    public void testNoActualTypeArguments_resolvePrimitives() {
        // Make sure we can still resolve concrete primitives, they don't have any type parameters.
        assertEquals(int.class, resolverWithoutActualTypes.resolveType(declaredFields[0].getGenericType()));
        assertEquals(int.class, resolverWithoutActualTypes.resolveType(declaredFields[0]));
    }

    @Test
    public void testNoActualTypeArguments_cannotResolveParameterizedTypes() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Cannot resolve type");
        resolverWithoutActualTypes.resolveType(declaredFields[1]);
    }

    @Test
    public void testActualTypeArguments_resolvePrimitives() {
        assertEquals(int.class, resolverWithActualTypes.resolveType(declaredFields[0].getGenericType()));
        assertEquals(int.class, resolverWithActualTypes.resolveType(declaredFields[0]));
    }

    @Test
    public void testActualTypeArguments_resolveTypeVariables() {
        assertEquals(String.class, resolverWithActualTypes.resolveType(declaredFields[1].getGenericType()));
        assertEquals(String.class, resolverWithActualTypes.resolveType(declaredFields[1]));

        assertEquals(Integer.class, resolverWithActualTypes.resolveType(declaredFields[2].getGenericType()));
        assertEquals(Integer.class, resolverWithActualTypes.resolveType(declaredFields[2]));
    }

    @Test
    public void testActualTypeArguments_resolveParameterizedType_oneType() {
        final Type resolvedType = resolverWithActualTypes.resolveType(declaredFields[3]);
        assertTrue(resolvedType instanceof ParameterizedType);
        final ParameterizedType resolvedParameterizedType = (ParameterizedType) resolvedType;
        assertEquals(OneParameterizedType.class, resolvedParameterizedType.getRawType());
        assertArrayEquals(new Type[]{String.class}, resolvedParameterizedType.getActualTypeArguments());
    }

    @Test
    public void testActualTypeArguments_resolveParameterizedType_twoTypes() {
        final Type resolvedType = resolverWithActualTypes.resolveType(declaredFields[4]);
        assertTrue(resolvedType instanceof ParameterizedType);
        final ParameterizedType resolvedParameterizedType = (ParameterizedType) resolvedType;
        assertEquals(TwoParameterizedTypes.class, resolvedParameterizedType.getRawType());
        assertArrayEquals(new Type[]{String.class, Integer.class}, resolvedParameterizedType.getActualTypeArguments());
    }

    @Test
    public void testActualTypeArguments_resolveParameterizedType_nestedParameterizedTypes() {
        final Type resolvedType = resolverWithActualTypes.resolveType(declaredFields[5]);
        assertTrue(resolvedType instanceof ParameterizedType);
        final ParameterizedType resolvedParameterizedType = (ParameterizedType) resolvedType;
        assertEquals(OneParameterizedType.class, resolvedParameterizedType.getRawType());
        assertArrayEquals(
                new Type[]{
                        new TypeResolver.ResolvedParameterizedType(
                                TwoParameterizedTypes.class,
                                new Type[]{Integer.class, String.class}
                        )
                }, resolvedParameterizedType.getActualTypeArguments());

        final Type[] resolvedActualTypeArguments = resolvedParameterizedType.getActualTypeArguments();
        assertEquals(1, resolvedActualTypeArguments.length);
        assertTrue(resolvedActualTypeArguments[0] instanceof TypeResolver.ResolvedParameterizedType);
        final TypeResolver.ResolvedParameterizedType resolvedParameterizedType_nested = (TypeResolver.ResolvedParameterizedType) resolvedActualTypeArguments[0];
        assertEquals(TwoParameterizedTypes.class, resolvedParameterizedType_nested.getRawType());
        assertArrayEquals(new Type[]{Integer.class, String.class}, resolvedParameterizedType_nested.getActualTypeArguments());
    }

    @Test
    public void testActualTypeTypeArguments_array() {
        final Type resolvedType = resolverWithActualTypes.resolveType(declaredFields[6]);
        assertTrue(resolvedType instanceof GenericArrayType);
        final GenericArrayType resolvedGenericArrayType = (GenericArrayType) resolvedType;
        assertEquals(String.class, resolvedGenericArrayType.getGenericComponentType());
    }

    @Test
    public void testActualTypeTypeArguments_arrayOfNestedTypes() {
        final Type resolvedType = resolverWithActualTypes.resolveType(declaredFields[7]);
        assertTrue(resolvedType instanceof GenericArrayType);
        final GenericArrayType resolvedGenericArrayType = (GenericArrayType) resolvedType;
        assertEquals(
                new TypeResolver.ResolvedParameterizedType(
                        OneParameterizedType.class,
                        new Type[] {Integer.class}
                ),
                resolvedGenericArrayType.getGenericComponentType());
    }

    @Test
    public void testRecursiveGenericClass() {
        final TypeResolver resolveWithRecursiveGenericClass = new TypeResolver(
                RecursiveClass.class,
                new Type[]{String.class});

        final Type resolvedField0 = resolveWithRecursiveGenericClass.resolveType(
                resolveWithRecursiveGenericClass.getDeclaredFields()[0]);
        assertEquals(String.class, resolvedField0);

        final Type resolvedField1 = resolveWithRecursiveGenericClass.resolveType(
                resolveWithRecursiveGenericClass.getDeclaredFields()[1]);
        assertEquals(
                new TypeResolver.ResolvedParameterizedType(
                        RecursiveClass.class,
                        new Type[]{String.class}
                ), resolvedField1);
    }

    @Test
    public void testWildcardTypesNotSupported() {
        final TypeResolver resolverWithWildcards = new TypeResolver(
                EnclosingWithWildcards.class,
                new Type[]{String.class});

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Wildcard types are not supported");
        resolverWithActualTypes.resolveType(resolverWithWildcards.getDeclaredFields()[0]);
    }

    // Class definitions below.

    private static class TestClass<T, U> {
        private int intType;
        private T tType;
        private U uType;
        private OneParameterizedType<T> nestedTType;
        private TwoParameterizedTypes<T, U> nestedTUTypes;
        private OneParameterizedType<TwoParameterizedTypes<U, T>> doubleNestedTypes;
        private T[] tArray;
        private OneParameterizedType<U>[] arrayOfNestedUTypes;
    }

    private static class OneParameterizedType<C> {
        private C value;
    }

    private static class TwoParameterizedTypes<A, B> {
        private A value1;
        private B value2;
    }

    private static class RecursiveClass<T> {
        private T data;
        private RecursiveClass<T> next;
    }

    private static class EnclosingWithWildcards<T> {
        private List<? extends T> listWithWildcards;
    }

}