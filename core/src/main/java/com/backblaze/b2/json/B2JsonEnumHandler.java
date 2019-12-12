/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2Preconditions;
import com.backblaze.b2.util.B2StringUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class B2JsonEnumHandler<T> implements B2JsonTypeHandler<T> {

    private final Class<T> enumClass;

    private final Method valueOfMethod;
    private final T defaultForInvalidValue; // may be null

    public B2JsonEnumHandler(Class<T> enumClass) throws B2JsonException {
        this.enumClass = enumClass;
        this.valueOfMethod = getValueOfMethod(enumClass);
        this.defaultForInvalidValue = getDefaultForInvalidValue(enumClass);
    }

    private Method getValueOfMethod(Class<T> enumClass) throws B2JsonException {
        B2Preconditions.checkArgument(enumClass.isEnum());
        try {
            return enumClass.getMethod("valueOf", String.class);
        }
        catch (NoSuchMethodException e) {
            throw new B2JsonException("enum class " + enumClass + " has no valueOf method");
        }
    }

    private T getDefaultForInvalidValue(Class<T> enumClass) throws B2JsonException {
        T defaultForInvalid = null;
        for (T valueConstant : enumClass.getEnumConstants()) {
            try {
                final Enum value = (Enum) valueConstant;
                final Field field = enumClass.getField(value.name());
                if (field.getAnnotation(B2Json.defaultForInvalidEnumValue.class) != null) {
                    if (defaultForInvalid != null) {
                        throw new B2JsonException("more than one @B2Json.defaultForInvalidEnumValue " +
                                "annotation in enum class " + enumClass.getCanonicalName());
                    }
                    //noinspection unchecked
                    defaultForInvalid = (T) value;
                }
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("failed to get field for enum value constant (" +
                        valueConstant + ") in enum class " + enumClass.getCanonicalName());
            }
        }
        return defaultForInvalid;
    }

    public Type getHandledType() {
        return enumClass;
    }

    public void serialize(T obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        out.writeString(obj.toString());
    }

    public T deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {
        String str = in.readString();
        return deserializeUrlParam(str);
    }

    public T deserializeUrlParam(String urlValue) throws B2JsonException {
        try {
            //noinspection unchecked
            return (T) valueOfMethod.invoke(null, urlValue);
        }
        catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof IllegalArgumentException) {
                if (defaultForInvalidValue != null) {
                    return defaultForInvalidValue;
                } else {
                    String validValues = B2StringUtil.join(", ", enumClass.getEnumConstants());
                    throw new B2JsonException(urlValue + " is not a valid value.  Valid values are: " + validValues);
                }
            }
            else {
                throw new B2JsonException("error calling " + enumClass + ".valueOf", e);
            }
        }
        catch (IllegalAccessException e) {
            throw new B2JsonException("error calling " + enumClass + ".valueOf", e);
        }
    }

    public T defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return true;
    }
}
