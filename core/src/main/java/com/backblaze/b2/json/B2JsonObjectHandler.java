/*
 * Copyright 2022, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.json.FieldInfo.FieldRequirement;
import com.backblaze.b2.util.B2Preconditions;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * (De)serializes Java objects based on field annotations.
 *
 * See doc comment on B2Json for annotation requirements.
 */
public class B2JsonObjectHandler<T> extends B2JsonTypeHandlerWithDefaults<T> {

    /**
     * The class of object we handle.
     */
    private final Class<T> clazz;
    private final B2TypeResolver typeResolver;

    /**
     * The union class that this one belongs to, or null if there is not one.
     */
    private final Class<?> unionClass;

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
    private FieldInfo [] fields;

    /**
     * Map from json member name to FieldInfo.
     */
    private final Map<String, FieldInfo> jsonMemberNameFieldInfoMap = new HashMap<>();

    /**
     * Map from Java object field name to FieldInfo
     */
    private final Map<String, FieldInfo> javaFieldNameFieldInfoMap = new HashMap<>();

    /**
     * The constructor to use.
     */
    private Constructor<T> constructor;

    /**
     * Number of parameters to constructor.
     */
    private int constructorParamCount;

    /**
     * Position of version parameter to constructor.
     */
    private Integer versionParamIndexOrNull;

    /**
     * Set containing the names of fields to discard during parsing.
     */
    private Set<String> fieldsToDiscard;

    /**
     * Sets up a new handler for this class based on reflection for the class.
     */
    /*package*/ B2JsonObjectHandler(Class<T> clazz) throws B2JsonException {
        this(clazz, null);
    }

    /*package*/ B2JsonObjectHandler(Class<T> clazz, Type[] actualTypeArguments) throws B2JsonException {

        this.clazz = clazz;
        this.typeResolver = new B2TypeResolver(clazz, actualTypeArguments);

        // Is this a member of a union type?
        {
            Class<?> unionClass = null;
            String fieldName = null;
            String fieldValue = null;
            for (Class<?> parent = clazz.getSuperclass(); parent != null; parent = parent.getSuperclass()) {
                if (B2JsonHandlerMap.isUnionBase(parent)) {
                    unionClass = parent;
                    fieldName = parent.getAnnotation(B2Json.union.class).typeField();
                    fieldValue = B2JsonUnionBaseHandler.getUnionTypeMap(parent).getTypeNameOrNullForClass(clazz);
                    if (fieldValue == null) {
                        throw new B2JsonException("class " + clazz + " inherits from " + parent + ", but is not in the type map");
                    }
                    break;
                }
            }
            this.unionClass = unionClass;
            this.unionTypeFieldName = fieldName;
            this.unionTypeFieldValue = fieldValue;
        }
    }

    protected void initializeImplementation(B2JsonHandlerMap handlerMap) throws B2JsonException {

        // If we're a member of a union, force creation of the union to do checking on it.
        // Even if the caller is just serializing this subclass, we want to make sure the whole
        // union is valid.
        if (unionClass != null) {
            handlerMap.getUninitializedHandler(unionClass);
        }

        // Get information on all of the fields in the class.
        for (Field field : B2JsonHandlerMap.getObjectFieldsForJson(clazz)) {
            final FieldRequirement requirement = B2JsonHandlerMap.getFieldRequirement(clazz, field);
            final Type resolvedFieldType = typeResolver.resolveType(field);
            final B2JsonTypeHandler<?> handler = handlerMap.getUninitializedHandler(resolvedFieldType);
            final String defaultValueJsonOrNull = getDefaultValueJsonOrNull(field);
            final VersionRange versionRange = getVersionRange(field);
            final boolean isSensitive = field.getAnnotation(B2Json.sensitive.class) != null;
            final boolean omitNull = omitNull(field);
            final boolean omitZero = omitZero(field);
            final B2Json.serializedName serializedNameAnnotation = field.getAnnotation(B2Json.serializedName.class);
            final String jsonMemberName = serializedNameAnnotation != null ? serializedNameAnnotation.value() : field.getName();
            final FieldInfo fieldInfo =
                    new FieldInfo(jsonMemberName, field, handler, requirement, defaultValueJsonOrNull, versionRange, isSensitive, omitNull, omitZero);

            if (jsonMemberNameFieldInfoMap.containsKey(jsonMemberName)) {
                throw new B2JsonException(clazz.getName() + " contains multiple class fields for the json member " + jsonMemberName);
            }
            jsonMemberNameFieldInfoMap.put(jsonMemberName, fieldInfo);
            javaFieldNameFieldInfoMap.put(field.getName(), fieldInfo);
        }
        fields = jsonMemberNameFieldInfoMap.values().toArray(new FieldInfo[jsonMemberNameFieldInfoMap.size()]);
        Arrays.sort(fields);

        this.constructor = B2JsonDeserializationUtil.findB2JsonConstructor(clazz);

        // Does the constructor take the version number as a parameter?
        final B2Json.constructor annotation = this.constructor.getAnnotation(B2Json.constructor.class);
        final String versionParamOrEmpty = annotation.versionParam();
        final int numberOfVersionParams = versionParamOrEmpty.isEmpty() ? 0 : 1;

        // Figure out the argument positions for the constructor.
        {
            // Parse @B2Json.constructor#params into an array
            String paramsWithCommas = annotation.params().replace(" ", "");
            String[] annotationParamNames = paramsWithCommas.split(",");
            if (annotationParamNames.length == 1 && annotationParamNames[0].length() == 0) {
                annotationParamNames = null;
            }

            // Verify that, if present, the number of params specified in the annotation is correct
            final int expectedParamCount = fields.length + numberOfVersionParams;
            if (annotationParamNames != null && annotationParamNames.length != expectedParamCount) {
                throw new B2JsonException(clazz.getName() + " constructor's @B2Json.constructor annotation does not have the right number of params.");
            }

            // Verify that the number of actual constructor params is correct
            Parameter[] constructorParams = this.constructor.getParameters();
            if (constructorParams.length != expectedParamCount) {
                throw new B2JsonException(clazz.getName() + " constructor does not have the right number of parameters");
            }

            Integer versionParamIndex = null;
            Set<String> paramNamesSeen = new HashSet<>();
            for (int i = 0; i < constructorParams.length; i++) {
                // Use annotated param names, if provided. Otherwise, attempt to use Java 8's real parameter name reflection
                String paramName;
                if (annotationParamNames != null) {
                    paramName = annotationParamNames[i];
                } else if (constructorParams[i].isNamePresent()) {
                    paramName = constructorParams[i].getName();
                } else {
                    throw new B2JsonException(clazz.getName() + " constructor is missing 'params' for its @B2Json.constructor annotation. Either specify this or add -parameters to javac args.");
                }

                if (paramNamesSeen.contains(paramName)) {
                    throw new B2JsonException(clazz.getName() + " constructor parameter '" + paramName + "' listed twice");
                }
                paramNamesSeen.add(paramName);
                if (paramName.isEmpty()) {
                    throw new B2JsonException(clazz.getName() + " constructor parameter name must not be empty");
                }
                if (paramName.equals(versionParamOrEmpty)) {
                    versionParamIndex = i;
                } else {
                    final FieldInfo fieldInfo = javaFieldNameFieldInfoMap.get(paramName);
                    if (fieldInfo == null) {
                        throw new B2JsonException(clazz.getName() + " param name is not a field: " + paramName);
                    }
                    fieldInfo.setConstructorArgIndex(i);
                }
            }
            this.versionParamIndexOrNull = versionParamIndex;
            this.constructorParamCount = constructorParams.length;
        }

        // figure out which names to discard, if any
        {
            this.fieldsToDiscard = B2JsonDeserializationUtil.getDiscards(this.constructor);
            for (String name : fieldsToDiscard) {
                final FieldInfo fieldInfo = javaFieldNameFieldInfoMap.get(name);
                if (fieldInfo != null && fieldInfo.requirement != FieldRequirement.IGNORED) {
                    throw new B2JsonException(clazz.getSimpleName() + "'s field '" + name + "' cannot be discarded: it's " + fieldInfo.requirement + ".  only non-existent or IGNORED fields can be discarded.");
                }
            }
        }
    }

    /**
     * Determines whether this field has the omitNull property.
     * This property can only be set from the 'optional' or
     * 'optionalWithDefault' annotations,
     * for all others omitNull will return false.
     * @param field field definition
     * @return whether the field has the omitNull property
     * @throws B2JsonException if omitNull is applied on to a field it shouldn't be.
     */
    private boolean omitNull(Field field) throws B2JsonException {
        final B2Json.optional optionalAnnotation = field.getAnnotation(B2Json.optional.class);
        final B2Json.optionalWithDefault optionalWithDefaultAnnotation = field.getAnnotation(B2Json.optionalWithDefault.class);

        final boolean omitNull;
        if (optionalAnnotation != null) {
            omitNull = optionalAnnotation.omitNull();
        } else if (optionalWithDefaultAnnotation != null) {
            omitNull = optionalWithDefaultAnnotation.omitNull();
        } else {
            omitNull = false;
        }
        // omitNull can only be set on non-primitive classes
        if (omitNull && field.getType().isPrimitive()) {
            final String message = String.format(
                    "Field %s.%s declared with 'omitNull = true' but is a primitive type",
                    this.clazz.getSimpleName(),
                    field.getName());
            throw new B2JsonException(message);
        }
        return omitNull;
    }

    /**
     * Determines whether this field has the omitZero property.
     * This property can only be set from the 'optional' or
     * 'optionalWithDefault' annotations,
     * for all others omitZero will return false.
     * @param field field definition
     * @return whether the field has the omitZero property
     * @throws B2JsonException if omitZero is applied on to a field it shouldn't be.
     */
    private boolean omitZero(Field field) throws B2JsonException {
        final B2Json.optional optionalAnnotation = field.getAnnotation(B2Json.optional.class);
        final B2Json.optionalWithDefault optionalWithDefaultAnnotation = field.getAnnotation(B2Json.optionalWithDefault.class);

        final boolean omitZero;
        if (optionalAnnotation != null) {
            omitZero = optionalAnnotation.omitZero();
        } else if (optionalWithDefaultAnnotation != null) {
            omitZero = optionalWithDefaultAnnotation.omitZero();
        } else {
            omitZero = false;
        }
        // omitZero can only be set on zeroable classes that B2Json innately knows about.
        if (omitZero && !isBuiltInZeroableType(field.getType())) {
            final String message = String.format(
                    "Field %s.%s declared with 'omitZero = true' but is not a primitive, numeric type",
                    this.clazz.getSimpleName(),
                    field.getName()
            );
            throw new B2JsonException(message);
        }
        return omitZero;
    }

    /**
     * Checks the validity of all of the default values for fields with optionalWithDefault.
     *
     * @throws B2JsonException if any are bad
     */
    @Override
    protected void checkDefaultValues() throws B2JsonException {
        for (FieldInfo field : jsonMemberNameFieldInfoMap.values()) {
            if (field.defaultValueJsonOrNull != null) {
                try {
                    field.handler.deserialize(
                            new B2JsonReader(new StringReader(field.defaultValueJsonOrNull)),
                            B2JsonOptions.DEFAULT
                    );
                } catch (B2JsonException | IOException e) {
                    throw new B2JsonException("error in default value for " +
                            clazz.getSimpleName() + "." + field.getJsonMemberName() + ": " +
                            e.getMessage());
                }
            }
        }
    }

    /**
     * Returns the serialized default value for a field, or null if it does not have one.
     */
    private String getDefaultValueJsonOrNull(Field field) {
        B2Json.optionalWithDefault optional = field.getAnnotation(B2Json.optionalWithDefault.class);
        if (optional == null) {
            return null;
        }
        else {
            return optional.defaultValue();
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

    public Type getHandledType() {
        return typeResolver.getType();
    }

    /**
     * Serializes the object, adding all fields to the JSON.
     *
     * Optional fields are always present, and set to null/0 when not present.
     *
     * The type name field for a member of a union type is added alphabetically in sequence, if needed.
     */
    public void serialize(T obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {

        B2Preconditions.checkState(isInitialized());
        throwIfBadDefaultValue();

        try {
            final int version = options.getVersion();
            boolean typeFieldDone = false;  // whether the type field for a member of a union type has been emitted
            out.startObject();
            if (fields != null) {
                for (FieldInfo fieldInfo : fields) {
                    if (unionTypeFieldName != null && !typeFieldDone && unionTypeFieldName.compareTo(fieldInfo.getJsonMemberName()) < 0) {
                        out.writeObjectFieldNameAndColon(unionTypeFieldName);
                        out.writeString(unionTypeFieldValue);
                        typeFieldDone = true;
                    }
                    if (fieldInfo.isInVersion(version)) {
                        final Object value = fieldInfo.field.get(obj);

                        // Only write the field if the value is not null OR omitNull is not set
                        final boolean omitValue =
                                (fieldInfo.omitNull && value == null) ||
                                (fieldInfo.omitZero && isZero(value));
                        if (!omitValue) {
                            out.writeObjectFieldNameAndColon(fieldInfo.getJsonMemberName());
                            if (fieldInfo.getIsSensitive() && options.getRedactSensitive()) {
                                out.writeString("***REDACTED***");
                            } else {
                                if (fieldInfo.isRequiredAndInVersion(version) && value == null) {
                                    throw new B2JsonException("required field " + fieldInfo.getJsonMemberName() + " cannot be null");
                                }
                                //noinspection unchecked
                                B2JsonUtil.serializeMaybeNull(fieldInfo.handler, value, out, options);
                            }
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

    public T deserializeUrlParam(String urlValue) throws B2JsonException {
        throw new B2JsonException("objects not supported in URL parameter");
    }

    public T deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {

        B2Preconditions.checkState(isInitialized());
        throwIfBadDefaultValue();

        if (fields == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserializes called with null fields");

        }

        final int version = options.getVersion();
        final Object [] constructorArgs = new Object [constructorParamCount];

        // Read the values that are present in the JSON.
        final BitSet foundFieldBits = new BitSet();
        if (in == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserialize called with null B2JsonReader");
        }
        if (in.startObjectAndCheckForContents()) {
            do {
                String fieldName = in.readObjectFieldNameAndColon();
                FieldInfo fieldInfo = jsonMemberNameFieldInfoMap.get(fieldName);
                if (fieldInfo == null) {
                    if ((options.getExtraFieldOption() == B2JsonOptions.ExtraFieldOption.ERROR) &&
                            (fieldsToDiscard == null || !fieldsToDiscard.contains(fieldName))) {
                        throw new B2JsonException("unknown field in " + clazz.getName() + ": " + fieldName);
                    }
                    in.skipValue();
                }
                else {
                    if (foundFieldBits.get(fieldInfo.constructorArgIndex)) {
                        throw new B2JsonException("duplicate field: " + fieldInfo.getJsonMemberName());
                    }
                    @SuppressWarnings("unchecked")
                    final Object value = B2JsonUtil.deserializeMaybeNull(fieldInfo.handler, in, options);
                    if (fieldInfo.isRequiredAndInVersion(version) && value == null) {
                        throw new B2JsonException("required field " + fieldInfo.getJsonMemberName() + " cannot be null");
                    }
                    constructorArgs[fieldInfo.constructorArgIndex] = value;
                    foundFieldBits.set(fieldInfo.constructorArgIndex);
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

        B2Preconditions.checkState(isInitialized());

        final int version = options.getVersion();
        final Object [] constructorArgs = new Object [fields.length];

        // Read the values that are present in the map.
        if (fieldNameToValue == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserializeFromFieldNameToValueMap called with null fieldNameToValue");

        }
        for (Map.Entry<String, Object> entry : fieldNameToValue.entrySet()) {
            String fieldName = entry.getKey();
            FieldInfo fieldInfo = jsonMemberNameFieldInfoMap.get(fieldName);
            if (fieldInfo == null) {
                if ((options.getExtraFieldOption() == B2JsonOptions.ExtraFieldOption.ERROR) &&
                        (fieldsToDiscard == null || !fieldsToDiscard.contains(fieldName))) {
                    throw new B2JsonException("unknown field in " + clazz.getName() + ": " + fieldName);
                }
            }
            else {
                Object value = entry.getValue();
                if (fieldInfo.isRequiredAndInVersion(version) && value == null) {
                    throw new B2JsonException("required field " + fieldInfo.getJsonMemberName() + " cannot be null");
                }
                constructorArgs[fieldInfo.constructorArgIndex] = value;
            }
        }
        return deserializeFromConstructorArgs(constructorArgs, version);
    }

    public T deserializeFromUrlParameterMap(Map<String, String> parameterMap, B2JsonOptions options) throws B2JsonException {

        B2Preconditions.checkState(isInitialized());

        final int version = options.getVersion();
        final Object [] constructorArgs = new Object [fields.length];

        // Read the values that are present in the parameter map.
        if (parameterMap == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserializeFromUrlParameterMape called with null parameterMap");

        }
        for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
            String fieldName = entry.getKey();
            String strOfValue = entry.getValue();

            FieldInfo fieldInfo = jsonMemberNameFieldInfoMap.get(fieldName);
            if (fieldInfo == null) {
                if ((options.getExtraFieldOption() == B2JsonOptions.ExtraFieldOption.ERROR) &&
                        (fieldsToDiscard == null || !fieldsToDiscard.contains(fieldName))) {
                    throw new B2JsonException("unknown field in " + clazz.getName() + ": " + fieldName);
                }
            }
            else {
                final Object value = fieldInfo.handler.deserializeUrlParam(strOfValue);
                if (fieldInfo.isRequiredAndInVersion(version) && value == null) {
                    throw new B2JsonException("required field " + fieldInfo.getJsonMemberName() + " cannot be null");
                }
                constructorArgs[fieldInfo.constructorArgIndex] = value;
            }
        }
        return deserializeFromConstructorArgs(constructorArgs, version);
    }

    private T deserializeFromConstructorArgs(Object[] constructorArgs, int version) throws B2JsonException {

        B2Preconditions.checkState(isInitialized());

        if (fields == null) {
            throw new B2JsonException("B2JsonObjectHandler.deserializeFromConstructorArgs called with null fields");
        }

        // Add default values for optional fields that are not present, and
        // check for required fields that are not present.
        for (FieldInfo fieldInfo : fields) {
            int index = fieldInfo.constructorArgIndex;
            if (constructorArgs[index] == null) {
                if (fieldInfo.isRequiredAndInVersion(version)) {
                    throw new B2JsonException("required field " + fieldInfo.getJsonMemberName() + " is missing");
                }
                if (fieldInfo.defaultValueJsonOrNull != null) {
                    // We do a fresh deserialization of the default value each time, in case it's
                    // a mutable type such as a List.
                    try {
                        constructorArgs[index] =
                                fieldInfo.handler.deserialize(
                                        new B2JsonReader(new StringReader(fieldInfo.defaultValueJsonOrNull)),
                                        B2JsonOptions.DEFAULT
                                );
                    } catch (IOException e) {
                        // This should never happen.  We should have checked default values
                        // in B2JsonHandlerMap right after initializing the handlers and before
                        // anybody tried to use them.  OTOH, if de-serializing a default value
                        // uses the default values in other types, we could hit this while
                        // checking default values in B2JsonHandlerMap.
                        throw new B2JsonException(e.getMessage());
                    }
                }
                else {
                    constructorArgs[index] = fieldInfo.handler.defaultValueForOptional();
                }
            }
            else {
                if (!fieldInfo.isInVersion(version)) {
                    throw new B2JsonException("field " + fieldInfo.getJsonMemberName() + " is not in version " + version);
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
    public static boolean isBuiltInZeroableType(Class<?> type) {
        return (type == byte.class) ||
                (type == int.class) ||
                (type == long.class) ||
                (type == float.class) ||
                (type == double.class);
    }

    public static boolean isZero(Object value) {
        B2Preconditions.checkArgumentIsNotNull(value, value); // because we should only be called for primitives!
        if (value instanceof Byte) {
            return ((Byte) value) == 0;
        }
        if (value instanceof Integer) {
            return ((Integer) value) == 0;
        }
        if (value instanceof Long) {
            return ((Long) value) == 0;
        }
        if (value instanceof Float) {
            return ((Float) value) == 0;
        }
        if (value instanceof Double) {
            return ((Double) value) == 0;
        }
        throw new RuntimeException("bug: isZero called on " + value.getClass());
    }
}
