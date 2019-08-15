/*
 * Copyright 2018, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2Preconditions;

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for the class that is the base class for a union type.
 *
 * This handler is used only for deserialization, where it finds the
 * type name in the JSON object, and this dispatches to the subclass
 * for that type.
 */
public class B2JsonUnionBaseHandler<T> extends B2JsonNonUrlTypeHandler<T> {

    /**
     * The class of object we handle.
     */
    private final Class<T> clazz;

    /**
     * The name of the JSON field that holds the type name.
     */
    private final String typeNameField;

    /**
     * The default value to use when deserializing an object with an unknown type.
     */
    private final String defaultValueJsonOrNull;

    /**
     * Mapping from type name (in the type name field of a serialized object) to class.
     */
    private Map<String, B2JsonObjectHandler<?>> typeNameToHandler;

    /**
     * Mapping from registered classes to their handlers.
     */
    private Map<Class<?>, B2JsonObjectHandler<?>> classToHandler;

    /**
     * Handlers for all of the fields in all of the subclasses.
     *
     * The rule is that in all of the subclasses of a union base class, all fields
     * with the same name must be of the same type.  This allows us to de-serialize
     * the fields before we know which subclass they belong to.
     */
    private Map<String, B2JsonTypeHandler<?>> fieldNameToHandler;


    /*package*/ B2JsonUnionBaseHandler(Class<T> clazz) throws B2JsonException {

        this.clazz = clazz;

        // Union classes must not inherit from other union classes.
        for (Class<?> parent = clazz.getSuperclass(); parent != null; parent = parent.getSuperclass()) {
            if (hasB2JsonAnnotation(parent)) {
                throw new B2JsonException("union class " + clazz + " inherits from another class with a B2Json annotation: " + parent);
            }
        }

        // Union base classes must not have any fields or constructors with B2Json annotations.
        for (Field field : clazz.getFields()) {
            if (hasB2JsonAnnotation(field)) {
                throw new B2JsonException("class " + clazz + ": field annotations not allowed in union class");
            }
        }
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (hasB2JsonAnnotation(constructor)) {
                throw new B2JsonException("class " + clazz + ": constructor annotations not allowed in union class");
            }
        }

        // Get the name of the field that holds the type.
        final B2Json.union union = clazz.getAnnotation(B2Json.union.class);
        this.typeNameField = union.typeField();

        // Get the default value, if there is one.
        final B2Json.defaultForUnknownType defaultForUnknownType =
                clazz.getAnnotation(B2Json.defaultForUnknownType.class);
        if (defaultForUnknownType == null) {
            defaultValueJsonOrNull = null;
        } else {
            defaultValueJsonOrNull = defaultForUnknownType.value();
        }
    }

    @Override
    protected void initializeImplementation(B2JsonHandlerMap b2JsonHandlerMap) throws B2JsonException {

        // Get the map of type name to class of all the members of the union.
        final Map<String, Class<?>> typeNameToClass = getUnionTypeMap(clazz).getTypeNameToClass();

        // Build the maps from type name and class to handler.
        typeNameToHandler = new HashMap<>();
        classToHandler = new HashMap<>();
        for (Map.Entry<String, Class<?>> entry : typeNameToClass.entrySet()) {
            final String typeName = entry.getKey();
            final Class<?> typeClass = entry.getValue();
            if (!hasSuperclass(typeClass, clazz)) { // use clazz.isAssignableFrom(typeClass)?
                throw new B2JsonException(typeClass + " is not a subclass of " + clazz);
            }
            final B2JsonTypeHandler<?> handler = b2JsonHandlerMap.getUninitializedHandler(typeClass);
            if (handler instanceof B2JsonObjectHandler) {
                typeNameToHandler.put(typeName, (B2JsonObjectHandler) handler);
                classToHandler.put(typeClass, (B2JsonObjectHandler) handler);
            }
            else {
                throw new B2JsonException("BUG: handler for subclass of union is not B2JsonObjectHandler");
            }
        }

        // Build the mapping from field name to handler.  It's an error for one field to have
        // more than one different type.
        fieldNameToHandler = new HashMap<>();
        final Map<String, String> fieldNameToSourceClassName = new HashMap<>();
        for (Class<?> subclass : typeNameToClass.values()) {
            for (Field field : B2JsonHandlerMap.getObjectFieldsForJson(subclass)) {
                final String fieldName = field.getName();
                final B2JsonTypeHandler handler =
                        B2JsonHandlerMap.getUninitializedFieldHandler(
                                field.getGenericType(),
                                b2JsonHandlerMap
                        );
                if (fieldNameToHandler.containsKey(fieldName)) {
                    // We have seen this field name before.  Throw an error if the type is different
                    // than before.
                    if (handler != fieldNameToHandler.get(fieldName)) {
                        throw new B2JsonException(
                                "In union type " + clazz + ", field " + fieldName + " has two different types.  " +
                                        fieldNameToSourceClassName.get(fieldName) + " has " +
                                        fieldNameToHandler.get(fieldName).getHandledClass() + " and " +
                                        subclass.toString() + " has " + handler.getHandledClass()
                        );
                    }
                }
                else {
                    // We have not seen this field name before.  Remember its type, and remember
                    // what class it came from, in case we need that info for an error message.
                    fieldNameToHandler.put(fieldName, handler);
                    fieldNameToSourceClassName.put(fieldName, subclass.toString());
                }
            }
        }
    }

    /**
     * Check the validity of the default value, if there is one.
     *
     * @throws B2JsonException if there is a problem
     */
    @Override
    void checkDefaultValues() throws B2JsonException {
        if (defaultValueJsonOrNull != null) {
            try {
                deserialize(
                        new B2JsonReader(new StringReader(defaultValueJsonOrNull)),
                        B2JsonOptions.DEFAULT
                );
            } catch (B2JsonException | IOException e) {
                throw new B2JsonException("error in default value for union " +
                        clazz.getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Returns true iff there are any B2Json annotations on this element.
     */
    private static boolean hasB2JsonAnnotation(AnnotatedElement element) {

        // My first approach was to get all the annotations, get their classes,
        // and see what package they are in.  That didn't work because getting
        // the class of an annotation returns a weird proxy that looks like
        // class com.sun.proxy.$Proxy6.
        //
        // The new plan is to simply test for all known annotations.

        for (Class<? extends Annotation> annotationClass : B2Json.ALL_ANNOTATIONS) {
            if (element.getAnnotation(annotationClass) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the first class has the second class as a direct or indirect superclass.
     */
    private static boolean hasSuperclass(Class<?> classA, Class<?> classB) {
        final Class<?> classASuper = classA.getSuperclass();
        // Superclass of Object is null.
        if (classASuper == null) {
            return false;
        }
        // Is it a direct or indirect superclass?
        return (classASuper == classB) || hasSuperclass(classASuper, classB);
    }


    /**
     * Returns the mapping from type name to class for all members of the union.
     *
     * Gets the map by calling the static method getUnionTypeMap on the base class.
     */
    /*package*/ static B2JsonUnionTypeMap getUnionTypeMap(Class<?> clazz) throws B2JsonException {
        // This uses getDeclaredMethod instead of just getMethod so that classes
        // can't inherit the type handler from their superclass.  that seems like
        // a safer starting point.
        Method method = null;
        try {
            method = clazz.getDeclaredMethod("getUnionTypeMap");
            method.setAccessible(true);
            final Object obj = method.invoke(null);
            if (!(obj instanceof B2JsonUnionTypeMap)) {
                throw new B2JsonException(clazz.getSimpleName() + "." + method.getName() + "() did not return a B2JsonUnionTypeMap.  It returned a " + obj.getClass());
            }
            return (B2JsonUnionTypeMap) obj;
        } catch (NoSuchMethodException e) {
            throw new B2JsonException("union base class " + clazz + " does not have a method getUnionTypeMap");
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof B2JsonException) {
                throw (B2JsonException) e.getCause();
            }
            throw new B2JsonException("failed to invoke " + method + ": " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new B2JsonException("illegal access to " + method + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Class<T> getHandledClass() {
        return clazz;
    }

    @Override
    public void serialize(T obj, B2JsonOptions options, B2JsonWriter out) throws IOException, B2JsonException {

        B2Preconditions.checkState(isInitialized());

        if (obj.getClass() == clazz) {
            // the union base class is basically "abstract" and can't be serialized.
            throw new B2JsonException("" + clazz + " is a union base class, and cannot be serialized");
        }

        //
        // we need to use the handler for obj's class to serialize obj.
        //

        // look in our map for the handler.
        //noinspection unchecked
        final B2JsonObjectHandler<T> objHandler = (B2JsonObjectHandler<T>) classToHandler.get(obj.getClass());
        if (objHandler == null) {
            throw new B2JsonException("" + obj.getClass() + " isn't a registered part of union " + clazz);
        }

        // we have the right handler, so make it do the work.
        objHandler.serialize(obj, options, out);
    }

    @Override
    public T deserialize(B2JsonReader in, B2JsonOptions options) throws B2JsonException, IOException {

        B2Preconditions.checkState(isInitialized());

        // Place to hold the name of one of the unknown fields, if there are any.
        // We don't want to throw an error about them until we're sure the type is
        // known.  And we only need the name of one to throw an exception, so there
        // is no need to keep a list of them.
        String unknownFieldNameOrNull = null;

        // Gather the values of all fields present, and also the name of the type of object to create.
        String typeName = null;
        final Map<String, Object> fieldNameToValue = new HashMap<>();
        if (in.startObjectAndCheckForContents()) {
            do {
                final String fieldName = in.readObjectFieldNameAndColon();
                if (typeNameField.equals(fieldName)) {
                    typeName = in.readString();
                }
                else {
                    final B2JsonTypeHandler<?> handler = fieldNameToHandler.get(fieldName);
                    if (handler == null) {
                        unknownFieldNameOrNull = fieldName;
                        in.skipValue();
                    }
                    else {
                        // we allow all fields to be parsed as null here.
                        // if it's a required field, we detect that in deserializeFromFieldNameToValueMap().
                        fieldNameToValue.put(fieldName, B2JsonUtil.deserializeMaybeNull(handler, in, options));
                    }
                }
            } while (in.objectHasMoreFields());
        }
        in.finishObject();

        // There should have been a type name
        if (typeName == null) {
            throw new B2JsonException("missing '" + typeNameField + "' in " + clazz.getSimpleName());
        }

        // Get the handler for this type.
        final B2JsonObjectHandler<?> handler = typeNameToHandler.get(typeName);
        if (handler == null) {
            if (defaultValueJsonOrNull == null) {
                throw new B2JsonException("unknown '" + typeNameField + "' in " + clazz.getSimpleName() + ": '" +
                        typeName + "'");
            } else {
                return deserialize(
                        new B2JsonReader(new StringReader(defaultValueJsonOrNull)),
                        B2JsonOptions.DEFAULT
                );
            }
        }

        // Throw errors for unknown fields.  (Actually, we'll just throw for one.)
        if (unknownFieldNameOrNull != null) {
            throw new B2JsonException("unknown field '" + unknownFieldNameOrNull + "' in union type " + clazz.getSimpleName());
        }
        
        // Let the handler build the resulting object.
        //noinspection unchecked
        return (T) handler.deserializeFromFieldNameToValueMap(fieldNameToValue, options);
    }

    @Override
    public T defaultValueForOptional() {
        return null;
    }

    @Override
    public boolean isStringInJson() {
        return false;
    }
}
