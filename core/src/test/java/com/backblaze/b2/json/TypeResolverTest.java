/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.json;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TypeResolverTest {


    @Test
    public void testNoActualTypeArguments() {
        TypeResolver typeResolver = new TypeResolver(TestClass.class);

        Field[] declaredFields = typeResolver.getDeclaredFields();
        assertEquals(6, declaredFields.length);

        assertEquals(int.class, typeResolver.resolveType(declaredFields[0].getGenericType()));
    }

    @Test
    public void testActualTypeArguments() {
        TypeResolver typeResolver = new TypeResolver(TestClass.class, new Type[]{String.class, Integer.class});

        Field[] declaredFields = typeResolver.getDeclaredFields();
        assertEquals(6, declaredFields.length);

        assertEquals(int.class, typeResolver.resolveType(declaredFields[0]));
        assertEquals(int.class, typeResolver.resolveType(declaredFields[0].getGenericType()));

        assertEquals(String.class, typeResolver.resolveType(declaredFields[1]));
        assertEquals(String.class, typeResolver.resolveType(declaredFields[1].getGenericType()));

        assertEquals(Integer.class, typeResolver.resolveType(declaredFields[2]));
        assertEquals(Integer.class, typeResolver.resolveType(declaredFields[2].getGenericType()));

        {
            final Type resolvedType = typeResolver.resolveType(declaredFields[3]);
            assertTrue(resolvedType instanceof ParameterizedType);
            final ParameterizedType resolvedParameterizedType = (ParameterizedType)resolvedType;
            assertEquals(OneParameterizedType.class, resolvedParameterizedType.getRawType());
            assertArrayEquals(new Type[] {String.class}, resolvedParameterizedType.getActualTypeArguments());
        }

        {
            final Type resolvedType = typeResolver.resolveType(declaredFields[4]);
            assertTrue(resolvedType instanceof ParameterizedType);
            final ParameterizedType resolvedParameterizedType = (ParameterizedType)resolvedType;
            assertEquals(TwoParameterizedTypes.class, resolvedParameterizedType.getRawType());
            assertArrayEquals(new Type[] {String.class, Integer.class}, resolvedParameterizedType.getActualTypeArguments());
        }

        {
            final Type resolvedType = typeResolver.resolveType(declaredFields[5]);
            assertTrue(resolvedType instanceof ParameterizedType);
            final ParameterizedType resolvedParameterizedType = (ParameterizedType)resolvedType;
            assertEquals(OneParameterizedType.class, resolvedParameterizedType.getRawType());

            final Type[] resolvedActualTypeArguments = resolvedParameterizedType.getActualTypeArguments();
            assertEquals(1, resolvedActualTypeArguments.length);
            assertTrue(resolvedActualTypeArguments[0] instanceof TypeResolver.ResolvedParameterizedType);
            final TypeResolver.ResolvedParameterizedType resolvedParameterizedType_nested = (TypeResolver.ResolvedParameterizedType)resolvedActualTypeArguments[0];
            assertEquals(TwoParameterizedTypes.class, resolvedParameterizedType_nested.getRawType());
            assertArrayEquals(new Type[] {Integer.class, String.class}, resolvedParameterizedType_nested.getActualTypeArguments());
        }



    }

    private static class TestClass<T, U> {
        private int intType;
        private T tType;
        private U uType;
        private OneParameterizedType<T> nestedTType;
        private TwoParameterizedTypes<T, U> nestedTUTypes;
        private OneParameterizedType<TwoParameterizedTypes<U, T>> doubleNestedTypes;
    }

    private static class OneParameterizedType<C> {
        private C value;
    }

    private static class TwoParameterizedTypes<A, B> {
        private A value1;
        private B value2;
    }

}