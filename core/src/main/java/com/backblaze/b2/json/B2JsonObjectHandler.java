/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.json.FieldInfo.FieldRequirement;
import com.backblaze.b2.util.B2Collections;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

/**
 * (De)serializes Java objects based on field annotations.
 *
 * See doc comment on B2Json for annotation requirements.
 */
public class B2JsonObjectHandler<T> extends B2JsonNonUrlTypeHandler<T> {

    /**
     * The class of object we handle.
     */
    private final Class<T> clazz;

    /**
     * All of the non-static fields of the class, in alphabetical order.
     */
    private final FieldInfo [] fields;

    /**
     * Map from field name to field.
     */
    private final Map<String, FieldInfo> fieldMap  = new HashMap<>();

    /**
     * The constructor to use.
     */
    private final Constructor<T> constructor;

    /**
     * Bit mask of all required fields.
     */
    private final long requiredBitMask;

    /**
     * null or a set containing the names of fields to discard during parsing.
     */
    private final Set<String> fieldsToDiscard;

    /**
     * Sets up a new handler for this class based on reflection for the class.
     */
    /*package*/ B2JsonObjectHandler(Class<T> clazz, B2JsonHandlerMap handlerMap) throws B2JsonException {
        this.clazz = clazz;

        // Add the B2JsonObjectHandler for this class into to the handlerMap before descending into the class's
        // fields, so that if it's encountered recursively (such as in a tree structure), then it's used to
        // describe the recursion instead of following that recursion forever.. or at least until the stack
        // overflows.
        //
        // See comment on rememberHandler() about thread safety.
        handlerMap.rememberHandler(clazz, this);

        // Get information on all of the fields in the class.
        for (Field field : clazz.getDeclaredFields()) {
            FieldRequirement requirement = getFieldRequirement(field);
            if (!Modifier.isStatic(field.getModifiers()) && requirement != FieldRequirement.IGNORED) {
                B2JsonTypeHandler<?> handler = getFieldHandler(field.getGenericType(), handlerMap);
                Object defaultValueOrNull = getDefaultValueOrNull(field, handler);
                FieldInfo fieldInfo = new FieldInfo(field, handler, requirement, defaultValueOrNull);
                fieldMap.put(field.getName(), fieldInfo);
            }
        }
        fields = fieldMap.values().toArray(new FieldInfo [fieldMap.size()]);
        Arrays.sort(fields);

        // Find the constructor to use.
        Constructor<T> chosenConstructor = null;
        for (Constructor<?> candidate : clazz.getDeclaredConstructors()) {
            if (candidate.getAnnotation(B2Json.constructor.class) != null) {
                if (chosenConstructor != null) {
                    throw new B2JsonException(clazz.getName() + " has two constructors selected");
                }
                //noinspection unchecked
                chosenConstructor = (Constructor<T>) candidate;
                chosenConstructor.setAccessible(true);
            }
        }
        if (chosenConstructor == null) {
            throw new B2JsonException(clazz.getName() + " has no constructor annotated with B2Json.constructor");
        }
        this.constructor = chosenConstructor;

        // Figure out the argument positions for the constructor.
        final B2Json.constructor annotation = chosenConstructor.getAnnotation(B2Json.constructor.class);
        {
            String paramsWithCommas = annotation.params().replace(" ", "");
            String [] paramNames = paramsWithCommas.split(",");
            if (paramNames.length == 1 && paramNames[0].length() == 0) {
                paramNames = new String [0];
            }

            if (paramNames.length != fields.length) {
                throw new IllegalArgumentException(clazz.getName() + " constructor does not have the right number of parameters");
            }

            int bitMask = 0;
            for (int i = 0; i < paramNames.length; i++) {
                String paramName = paramNames[i];
                final FieldInfo fieldInfo = fieldMap.get(paramName);
                if (fieldInfo == null) {
                    throw new B2JsonException(clazz.getName() + " param name is not a field: " + paramName);
                }
                fieldInfo.setConstructorArgIndex(i);
                if (fieldInfo.requirement == FieldRequirement.REQUIRED) {
                    bitMask |= fieldInfo.bit;
                }
            }
            this.requiredBitMask = bitMask;
        }

        // figure out which names to discard, if any
        {
            String discardsWithCommas = annotation.discards().replace(" ", "");
            if (discardsWithCommas.isEmpty()) {
                fieldsToDiscard = null;
            } else {
                String[] discardNames = discardsWithCommas.split(",");
                fieldsToDiscard = B2Collections.unmodifiableSet(discardNames);
                for (String name : fieldsToDiscard) {
                    final FieldInfo fieldInfo = fieldMap.get(name);
                    if (fieldInfo != null && fieldInfo.requirement != FieldRequirement.IGNORED) {
                        throw new B2JsonException(clazz.getSimpleName() + "'s field '" + name + "' cannot be discarded: it's " + fieldInfo.requirement + ".  only non-existent or IGNORED fields can be discarded.");
                    }
                }
            }
        }
    }

    /**
     * Returns the information about all fields in the object.
     */
    /*package*/  Map<String, FieldInfo> getFieldMap() {
        return fieldMap;
    }

    private Object getDefaultValueOrNull(Field field, B2JsonTypeHandler<?> handler) throws B2JsonException {
        B2Json.optionalWithDefault optional = field.getAnnotation(B2Json.optionalWithDefault.class);
        if (optional == null) {
            return null;
        }
        else {
            String jsonOfDefaultValue = optional.defaultValue();
            try {
                B2JsonReader reader = new B2JsonReader(new StringReader(jsonOfDefaultValue));
                return handler.deserialize(reader, 0);
            }
            catch (IOException e) {
                throw new B2JsonException("error reading default value", e);
            }
        }
    }

    private B2JsonTypeHandler getFieldHandler(Type fieldType, B2JsonHandlerMap handlerMap) throws B2JsonException {
        if (fieldType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) fieldType;
            final Class rawType = (Class) paramType.getRawType();
            if (rawType == LinkedHashSet.class) {
                Type itemType = paramType.getActualTypeArguments()[0];
                B2JsonTypeHandler<?> itemHandler = getFieldHandler(itemType, handlerMap);
                return new B2JsonLinkedHashSetHandler(itemHandler);
            }
            if (rawType == List.class) {
                Type itemType = paramType.getActualTypeArguments()[0];
                B2JsonTypeHandler<?> itemHandler = getFieldHandler(itemType, handlerMap);
                return new B2JsonListHandler(itemHandler);
            }
            if (rawType == TreeSet.class) {
                Type itemType = paramType.getActualTypeArguments()[0];
                B2JsonTypeHandler<?> itemHandler = getFieldHandler(itemType, handlerMap);
                return new B2JsonTreeSetHandler(itemHandler);
            }
            if (rawType == Set.class) {
                Type itemType = paramType.getActualTypeArguments()[0];
                B2JsonTypeHandler<?> itemHandler = getFieldHandler(itemType, handlerMap);
                return new B2JsonSetHandler(itemHandler);
            }
            if (rawType == EnumSet.class) {
                Type itemType = paramType.getActualTypeArguments()[0];
                B2JsonTypeHandler<?> itemHandler = getFieldHandler(itemType, handlerMap);
                return new B2JsonEnumSetHandler(itemHandler);
            }
            if (rawType == Map.class || rawType == TreeMap.class) {
                Type keyType = paramType.getActualTypeArguments()[0];
                Type valueType = paramType.getActualTypeArguments()[1];
                B2JsonTypeHandler<?> keyHandler = getFieldHandler(keyType, handlerMap);
                B2JsonTypeHandler<?> valueHandler = getFieldHandler(valueType, handlerMap);
                return new B2JsonMapHandler(keyHandler, valueHandler);
            }
            if (rawType == ConcurrentMap.class) {
                Type keyType = paramType.getActualTypeArguments()[0];
                Type valueType = paramType.getActualTypeArguments()[1];
                B2JsonTypeHandler<?> keyHandler = getFieldHandler(keyType, handlerMap);
                B2JsonTypeHandler<?> valueHandler = getFieldHandler(valueType, handlerMap);
                return new B2JsonConcurrentMapHandler(keyHandler, valueHandler);
            }
        }
        if (fieldType instanceof Class) {
            final Class fieldClass = (Class) fieldType;
            //noinspection unchecked
            return handlerMap.getHandler(fieldClass);
        }
        throw new B2JsonException("Do not know how to handle: " + fieldType);
    }

    private FieldRequirement getFieldRequirement(Field field) throws B2JsonException {

        // We never handle static fields
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            return FieldRequirement.IGNORED;
        }

        // Get the annotation to see how we should handle it.
        FieldRequirement result = null;
        int count = 0;
        if (field.getAnnotation(B2Json.required.class) != null) {
            result = FieldRequirement.REQUIRED;
            count += 1;
        }
        if (field.getAnnotation(B2Json.optional.class) != null) {
            result = FieldRequirement.OPTIONAL;
            count += 1;
        }
        if (field.getAnnotation(B2Json.optionalWithDefault.class) != null) {
            result = FieldRequirement.OPTIONAL;
            count += 1;
        }
        if (field.getAnnotation(B2Json.ignored.class) != null) {
            result = FieldRequirement.IGNORED;
            count += 1;
        }
        if (count != 1) {
            throw new B2JsonException(clazz.getName() + "." + field.getName() + " should have exactly one annotation: required, optional, optionalWithDefault, or ignored");
        }
        return result;
    }

    public Class<T> getHandledClass() {
        return clazz;
    }

    public void serialize(T obj, B2JsonWriter out) throws IOException, B2JsonException {
        try {
            out.startObject();
            if (fields != null) {
                for (FieldInfo fieldInfo : fields) {
                    out.writeObjectFieldNameAndColon(fieldInfo.getName());
                    final Object value = fieldInfo.field.get(obj);
                    if (fieldInfo.requirement == FieldRequirement.REQUIRED && value == null) {
                        throw new B2JsonException("required field " + fieldInfo.getName() + " cannot be null");
                    }
                    //noinspection unchecked
                    B2JsonUtil.serializeMaybeNull(fieldInfo.handler, value, out);
                }
            }
            out.finishObject();
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public T deserialize(B2JsonReader in, int options) throws B2JsonException, IOException {
        if (fields == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserializes called with null fields");

        }
        Object [] constructorArgs = new Object [fields.length];

        // Read the values that are present in the JSON.
        long foundFieldBits = 0;
        if (in == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserialize called with null B2JsonReader");

        }
        if (in.startObjectAndCheckForContents()) {
            do {
                String fieldName = in.readObjectFieldNameAndColon();
                FieldInfo fieldInfo = fieldMap.get(fieldName);
                if (fieldInfo == null) {
                    if (((options & B2Json.ALLOW_EXTRA_FIELDS) == 0) &&
                            (fieldsToDiscard == null || !fieldsToDiscard.contains(fieldName))) {
                        throw new B2JsonException("unknown field in " + clazz.getName() + ": " + fieldName);
                    }
                    in.skipValue();
                }
                else {
                    if ((foundFieldBits & fieldInfo.bit) != 0) {
                        throw new B2JsonException("duplicate field: " + fieldInfo.getName());
                    }
                    @SuppressWarnings("unchecked")
                    final Object value = B2JsonUtil.deserializeMaybeNull(fieldInfo.handler, in, options);
                    if (fieldInfo.requirement == FieldRequirement.REQUIRED && value == null) {
                        throw new B2JsonException("required field " + fieldInfo.getName() + " cannot be null");
                    }
                    constructorArgs[fieldInfo.constructorArgIndex] = value;
                    foundFieldBits |= fieldInfo.bit;
                }
            } while (in.objectHasMoreFields());
        }
        in.finishObject();

        return deserializeFromConstructorArgs(constructorArgs, foundFieldBits);
    }

    public T deserializeFromFieldNameToValueMap(Map<String, Object> fieldNameToValue, int options) throws B2JsonException {
        Object [] constructorArgs = new Object [fields.length];

        // Read the values that are present in the map.
        long foundFieldBits = 0;
        if (fieldNameToValue == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserializeFromFieldNameToValueMap called with null fieldNameToValue");

        }
        for (Map.Entry<String, Object> entry : fieldNameToValue.entrySet()) {
            String fieldName = entry.getKey();
            FieldInfo fieldInfo = fieldMap.get(fieldName);
            if (fieldInfo == null) {
                if (((options & B2Json.ALLOW_EXTRA_FIELDS) == 0) &&
                        (fieldsToDiscard == null || !fieldsToDiscard.contains(fieldName))) {
                    throw new B2JsonException("unknown field in " + clazz.getName() + ": " + fieldName);
                }
            }
            else {
                Object value = entry.getValue();
                if (fieldInfo.requirement == FieldRequirement.REQUIRED && value == null) {
                    throw new B2JsonException("required field " + fieldInfo.getName() + " cannot be null");
                }
                constructorArgs[fieldInfo.constructorArgIndex] = value;
                foundFieldBits |= fieldInfo.bit;
            }
        }
        return deserializeFromConstructorArgs(constructorArgs, foundFieldBits);
    }

    public T deserializeFromUrlParameterMap(Map<String, String> parameterMap, int options) throws B2JsonException {
        Object [] constructorArgs = new Object [fields.length];

        // Read the values that are present in the parameter map.
        long foundFieldBits = 0;
        if (parameterMap == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserializeFromUrlParameterMape called with null parameterMap");

        }
        for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
            String fieldName = entry.getKey();
            String strOfValue = entry.getValue();

            FieldInfo fieldInfo = fieldMap.get(fieldName);
            if (fieldInfo == null) {
                if (((options & B2Json.ALLOW_EXTRA_FIELDS) == 0) &&
                        (fieldsToDiscard == null || !fieldsToDiscard.contains(fieldName))) {
                    throw new B2JsonException("unknown field in " + clazz.getName() + ": " + fieldName);
                }
            }
            else {
                final Object value = fieldInfo.handler.deserializeUrlParam(strOfValue);
                if (fieldInfo.requirement == FieldRequirement.REQUIRED && value == null) {
                    throw new B2JsonException("required field " + fieldInfo.getName() + " cannot be null");
                }
                constructorArgs[fieldInfo.constructorArgIndex] = value;
                foundFieldBits |= fieldInfo.bit;
            }
        }
        return deserializeFromConstructorArgs(constructorArgs, foundFieldBits);
    }

    private T deserializeFromConstructorArgs(Object[] constructorArgs, long foundFieldBits) throws B2JsonException {
        if (fields == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserializeFromConstructorArgs called with null fields");

        }
        // Add default values for optional fields that are not present.
        // Are there missing required fields?
        if (requiredBitMask != (requiredBitMask & foundFieldBits)) {
            for (FieldInfo fieldInfo : fields) {
                if (fieldInfo.requirement == FieldRequirement.REQUIRED && (fieldInfo.bit & foundFieldBits) == 0) {
                    throw new B2JsonException("required field " + fieldInfo.getName() + " is missing");
                }
            }
            throw new RuntimeException("bug: didn't find name of missing field");
        }
        for (FieldInfo fieldInfo : fields) {
            int index = fieldInfo.constructorArgIndex;
            if (constructorArgs[index] == null) {
                if (fieldInfo.defaultValueOrNull != null) {
                    constructorArgs[index] = fieldInfo.defaultValueOrNull;
                }
                else {
                    constructorArgs[index] = fieldInfo.handler.defaultValueForOptional();
                }
            }
        }

        try {
            return constructor.newInstance(constructorArgs);
        }
        catch (InstantiationException | IllegalAccessException e) {
            throw new B2JsonException(e.getMessage(), e);
        }
        catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof IllegalArgumentException) {
                throw new B2JsonBadValueException(targetException.getMessage());
            }
            else {
                throw new B2JsonException(targetException.getMessage(), targetException);
            }
        }
    }

    public T defaultValueForOptional() {
        return null;
    }

    public boolean isStringInJson() {
        return false;
    }
}
