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
import java.util.HashSet;
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
     * Non-null iff this class is the member of a union type.
     */
    private final String unionTypeFieldName;

    /**
     * Non-null iff this class is the member of a union type.
     */
    private final String unionTypeFieldValue;

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
     * Number of parameters to constructor.
     */
    private final int constructorParamCount;

    /**
     * Position of version parameter to constructor.
     */
    private final Integer versionParamIndexOrNull;

    /**
     * null or a set containing the names of fields to discard during parsing.
     */
    private final Set<String> fieldsToDiscard;

    /**
     * Sets up a new handler for this class based on reflection for the class.
     */
    /*package*/ B2JsonObjectHandler(Class<T> clazz, B2JsonHandlerMap handlerMap) throws B2JsonException {

        this.clazz = clazz;

        // Is this a member of a union type?
        {
            String fieldName = null;
            String fieldValue = null;
            for (Class<?> parent = clazz.getSuperclass(); parent != null; parent = parent.getSuperclass()) {
                if (B2JsonHandlerMap.isUnionBase(parent)) {
                    fieldValue = B2JsonUnionBaseHandler.getUnionTypeMap(parent).getTypeNameOrNullForClass(clazz);
                    if (fieldValue == null) {
                        throw new B2JsonException("class " + clazz + " inherits from " + parent + ", but is not in the type map");
                    }
                    fieldName = parent.getAnnotation(B2Json.union.class).typeField();
                    break;
                }
            }
            this.unionTypeFieldName = fieldName;
            this.unionTypeFieldValue = fieldValue;

        }

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
                final B2JsonTypeHandler<?> handler = getFieldHandler(field.getGenericType(), handlerMap);
                final Object defaultValueOrNull = getDefaultValueOrNull(field, handler);
                final VersionRange versionRange = getVersionRange(field);
                final boolean isSensitive = field.getAnnotation(B2Json.sensitive.class) != null;
                final FieldInfo fieldInfo = new FieldInfo(field, handler, requirement, defaultValueOrNull, versionRange, isSensitive);
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

        // Does the constructor take the version number as a parameter?
        final B2Json.constructor annotation = chosenConstructor.getAnnotation(B2Json.constructor.class);
        final String versionParamOrEmpty = annotation.versionParam();
        final int numberOfVersionParams = versionParamOrEmpty.isEmpty() ? 0 : 1;

        // Figure out the argument positions for the constructor.
        {
            String paramsWithCommas = annotation.params().replace(" ", "");
            String [] paramNames = paramsWithCommas.split(",");
            if (paramNames.length == 1 && paramNames[0].length() == 0) {
                paramNames = new String [0];
            }

            final int constructorParamCount = fields.length + numberOfVersionParams;
            if (paramNames.length != constructorParamCount) {
                throw new IllegalArgumentException(clazz.getName() + " constructor does not have the right number of parameters");
            }

            Integer versionParamIndex = null;
            Set<String> paramNamesSeen = new HashSet<>();
            for (int i = 0; i < paramNames.length; i++) {
                String paramName = paramNames[i];
                if (paramNamesSeen.contains(paramName)) {
                    throw new B2JsonException(clazz.getName() + " constructor parameter '" + paramName + "' listed twice");
                }
                paramNamesSeen.add(paramName);
                if (paramName.isEmpty()) {
                    throw new B2JsonException(clazz.getName() + " constructor parameter name must not be empty");
                }
                if (paramName.equals(versionParamOrEmpty)) {
                    versionParamIndex = i;
                }
                else {
                    final FieldInfo fieldInfo = fieldMap.get(paramName);
                    if (fieldInfo == null) {
                        throw new B2JsonException(clazz.getName() + " param name is not a field: " + paramName);
                    }
                    fieldInfo.setConstructorArgIndex(i);
                }
            }
            this.versionParamIndexOrNull = versionParamIndex;
            this.constructorParamCount = constructorParamCount;
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
                return handler.deserialize(reader, B2JsonOptions.DEFAULT);
            }
            catch (IOException e) {
                throw new B2JsonException("error reading default value", e);
            }
        }
    }

    /**
     * Returns the version range for a field.
     */
    private VersionRange getVersionRange(Field field) throws B2JsonException {
        final B2Json.firstVersion firstVersion = field.getAnnotation(B2Json.firstVersion.class);
        final B2Json.versionRange versionRange = field.getAnnotation(B2Json.versionRange.class);

        if (firstVersion != null && versionRange != null) {
            throw new B2JsonException("must not specify both 'firstVersion' and 'versionRange' in " + clazz);
        }


        if (firstVersion != null) {
            return VersionRange.allVersionsFrom(firstVersion.firstVersion());
        }
        else if (versionRange != null) {
            if (versionRange.lastVersion() < versionRange.firstVersion()) {
                throw new B2JsonException(
                        "last version " + versionRange.lastVersion() +
                        " is before first version " + versionRange.firstVersion() +
                        " in " + clazz
                );
            }
            return VersionRange.range(versionRange.firstVersion(), versionRange.lastVersion());
        }
        else {
            return VersionRange.ALL_VERSIONS;
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

    /**
     * Serializes the object, adding all fields to the JSON.
     *
     * Optional fields are always present, and set to null/0 when not present.
     *
     * The type name field for a member of a union type is added alphabetically in sequence, if needed.
     */
    public void serialize(T obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {
        try {
            final int version = options.getVersion();
            boolean typeFieldDone = false;  // whether the type field for a member of a union type has been emitted
            out.startObject();
            if (fields != null) {
                for (FieldInfo fieldInfo : fields) {
                    if (unionTypeFieldName != null && !typeFieldDone && unionTypeFieldName.compareTo(fieldInfo.getName()) < 0) {
                        out.writeObjectFieldNameAndColon(unionTypeFieldName);
                        out.writeString(unionTypeFieldValue);
                        typeFieldDone = true;
                    }
                    if (fieldInfo.isInVersion(version)) {
                        out.writeObjectFieldNameAndColon(fieldInfo.getName());
                        if (fieldInfo.getIsSensitive() && options.getRedactSensitive()) {
                            out.writeString("***REDACTED***");
                        } else {
                            final Object value = fieldInfo.field.get(obj);
                            if (fieldInfo.isRequiredAndInVersion(version) && value == null) {
                                throw new B2JsonException("required field " + fieldInfo.getName() + " cannot be null");
                            }
                            //noinspection unchecked
                            B2JsonUtil.serializeMaybeNull(fieldInfo.handler, value, out, options);
                        }
                    }
                }
            }
            if (unionTypeFieldName != null && !typeFieldDone) {
                out.writeObjectFieldNameAndColon(unionTypeFieldName);
                out.writeString(unionTypeFieldValue);
            }
            out.finishObject();
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public T deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {

        if (fields == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserializes called with null fields");

        }

        final int version = options.getVersion();
        final Object [] constructorArgs = new Object [constructorParamCount];

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
                    if ((options.getExtraFieldOption() == B2JsonOptions.ExtraFieldOption.ERROR) &&
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
                    if (fieldInfo.isRequiredAndInVersion(version) && value == null) {
                        throw new B2JsonException("required field " + fieldInfo.getName() + " cannot be null");
                    }
                    constructorArgs[fieldInfo.constructorArgIndex] = value;
                    foundFieldBits |= fieldInfo.bit;
                }
            } while (in.objectHasMoreFields());
        }
        in.finishObject();

        // Add the version number.
        if (versionParamIndexOrNull != null) {
            constructorArgs[versionParamIndexOrNull] = version;
        }

        return deserializeFromConstructorArgs(constructorArgs, version);
    }

    public T deserializeFromFieldNameToValueMap(Map<String, Object> fieldNameToValue, B2JsonOptions options) throws B2JsonException {

        final int version = options.getVersion();
        final Object [] constructorArgs = new Object [fields.length];

        // Read the values that are present in the map.
        long foundFieldBits = 0;
        if (fieldNameToValue == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserializeFromFieldNameToValueMap called with null fieldNameToValue");

        }
        for (Map.Entry<String, Object> entry : fieldNameToValue.entrySet()) {
            String fieldName = entry.getKey();
            FieldInfo fieldInfo = fieldMap.get(fieldName);
            if (fieldInfo == null) {
                if ((options.getExtraFieldOption() == B2JsonOptions.ExtraFieldOption.ERROR) &&
                        (fieldsToDiscard == null || !fieldsToDiscard.contains(fieldName))) {
                    throw new B2JsonException("unknown field in " + clazz.getName() + ": " + fieldName);
                }
            }
            else {
                Object value = entry.getValue();
                if (fieldInfo.isRequiredAndInVersion(version) && value == null) {
                    throw new B2JsonException("required field " + fieldInfo.getName() + " cannot be null");
                }
                constructorArgs[fieldInfo.constructorArgIndex] = value;
                foundFieldBits |= fieldInfo.bit;
            }
        }
        return deserializeFromConstructorArgs(constructorArgs, version);
    }

    public T deserializeFromUrlParameterMap(Map<String, String> parameterMap, B2JsonOptions options) throws B2JsonException {

        final int version = options.getVersion();
        final Object [] constructorArgs = new Object [fields.length];

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
                if ((options.getExtraFieldOption() == B2JsonOptions.ExtraFieldOption.ERROR) &&
                        (fieldsToDiscard == null || !fieldsToDiscard.contains(fieldName))) {
                    throw new B2JsonException("unknown field in " + clazz.getName() + ": " + fieldName);
                }
            }
            else {
                final Object value = fieldInfo.handler.deserializeUrlParam(strOfValue);
                if (fieldInfo.isRequiredAndInVersion(version) && value == null) {
                    throw new B2JsonException("required field " + fieldInfo.getName() + " cannot be null");
                }
                constructorArgs[fieldInfo.constructorArgIndex] = value;
                foundFieldBits |= fieldInfo.bit;
            }
        }
        return deserializeFromConstructorArgs(constructorArgs, version);
    }

    private T deserializeFromConstructorArgs(Object[] constructorArgs, int version) throws B2JsonException {
        if (fields == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserializeFromConstructorArgs called with null fields");
        }

        // Add default values for optional fields that are not present, and
        // check for required fields that are not present.
        for (FieldInfo fieldInfo : fields) {
            int index = fieldInfo.constructorArgIndex;
            if (constructorArgs[index] == null) {
                if (fieldInfo.isRequiredAndInVersion(version)) {
                    throw new B2JsonException("required field " + fieldInfo.getName() + " is missing");
                }
                if (fieldInfo.defaultValueOrNull != null) {
                    constructorArgs[index] = fieldInfo.defaultValueOrNull;
                }
                else {
                    constructorArgs[index] = fieldInfo.handler.defaultValueForOptional();
                }
            }
            else {
                if (!fieldInfo.isInVersion(version)) {
                    throw new B2JsonException("field " + fieldInfo.getName() + " is not in version " + version);
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
