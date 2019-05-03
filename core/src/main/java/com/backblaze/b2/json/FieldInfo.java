/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2Preconditions;

import java.lang.reflect.Field;

/**
 * Information for one field in an object that is (de)serialized.
 *
 * Used by B2ObjectHandler and B2JsonObjectHandler.
 */
public final class FieldInfo implements Comparable<FieldInfo> {

    public enum FieldRequirement { REQUIRED, OPTIONAL, IGNORED }

    public final Field field;
    public final B2JsonTypeHandler handler;
    public final FieldRequirement requirement;
    public final Object defaultValueOrNull;
    public final VersionRange versionRange;
    public int constructorArgIndex;
    public long bit;
    public final boolean isSensitive;

    /*package*/ FieldInfo(
            Field field, B2JsonTypeHandler<?> handler,
            FieldRequirement requirement,
            Object defaultValueOrNull,
            VersionRange versionRange,
            boolean isSensitive
    ) {
        this.field = field;
        this.handler =  handler;
        this.requirement = requirement;
        this.defaultValueOrNull = defaultValueOrNull;
        this.versionRange = versionRange;
        this.isSensitive = isSensitive;

        this.field.setAccessible(true);
    }

    public String getName() {
        return field.getName();
    }

    public B2JsonTypeHandler getHandler() {
        return handler;
    }

    public boolean getIsSensitive() {
        return isSensitive;
    }

    public int compareTo(@SuppressWarnings("NullableProblems") FieldInfo o) {
        return field.getName().compareTo(o.field.getName());
    }

    public void setConstructorArgIndex(int index) {
        B2Preconditions.checkArgument(index < 64);
        constructorArgIndex = index;
        bit = 1L << index;
    }

    public boolean isInVersion(int version) {
        return versionRange.includesVersion(version);
    }

    public boolean isRequiredAndInVersion(int version) {
        return requirement == FieldRequirement.REQUIRED && versionRange.includesVersion(version);
    }

}
