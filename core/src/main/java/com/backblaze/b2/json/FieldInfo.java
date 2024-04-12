/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import java.lang.reflect.Field;

/**
 * Information for one field in an object that is (de)serialized.
 * <p>
 * Used by B2ObjectHandler and B2JsonObjectHandler.
 */
public final class FieldInfo implements Comparable<FieldInfo> {

    public enum FieldRequirement { REQUIRED, OPTIONAL, IGNORED }

    private final String jsonMemberName;
    public final Field field;
    public final B2JsonTypeHandler handler;
    public final FieldRequirement requirement;
    public final String defaultValueJsonOrNull;
    public final VersionRange versionRange;
    public int constructorArgIndex;
    public final boolean isSensitive;
    public final boolean omitNull;
    public final boolean omitZero;

    /*package*/ FieldInfo(
            String jsonMemberName,
            Field field, B2JsonTypeHandler<?> handler,
            FieldRequirement requirement,
            String defaultValueJsonOrNull,
            VersionRange versionRange,
            boolean isSensitive,
            boolean omitNull,
            boolean omitZero) {
        this.jsonMemberName = jsonMemberName;
        this.field = field;
        this.handler =  handler;
        this.requirement = requirement;
        this.defaultValueJsonOrNull = defaultValueJsonOrNull;
        this.versionRange = versionRange;
        this.isSensitive = isSensitive;
        this.omitNull = omitNull;
        this.omitZero = omitZero;

        this.field.setAccessible(true);
    }

    /**
     * Returns the member name that this field is serialized to in Json.
     * @deprecated use {@link #getJsonMemberName()} instead which is clearer.
     */
    @Deprecated
    public String getName() {
        return jsonMemberName;
    }

    /**
     * Returns the member name that this field is serialized to in Json.
     */
    public String getJsonMemberName() {
        return jsonMemberName;
    }

    public B2JsonTypeHandler getHandler() {
        return handler;
    }

    public boolean getIsSensitive() {
        return isSensitive;
    }

    public int compareTo(@SuppressWarnings("NullableProblems") FieldInfo o) {
        return jsonMemberName.compareTo(o.jsonMemberName);
    }

    public void setConstructorArgIndex(int index) {
        constructorArgIndex = index;
    }

    public boolean isInVersion(int version) {
        return versionRange.includesVersion(version);
    }

    public boolean isRequiredAndInVersion(int version) {
        return requirement == FieldRequirement.REQUIRED && versionRange.includesVersion(version);
    }

}
