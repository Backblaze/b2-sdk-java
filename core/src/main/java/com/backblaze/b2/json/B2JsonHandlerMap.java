/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2Preconditions;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * Holds a mapping from Class to B2JsonTypeHandler.
 *
 * The mapping starts out with initial contents, which must be ALL
 * of the non-default mappings that will be used.  If any other
 * handlers are needed, the default B2JsonObjectHandler will be used
 * for that class.
 *
 * This class is THREAD SAFE.
 */
public class B2JsonHandlerMap {

    // access to this map is always synchronized on this object.
    // we think it's safe to overwrite the entry for a given class because
    // we assume all handlers are stateless and any two instances of
    // a handler for a class are equivalent.
    private final Map<Class<?>, B2JsonTypeHandler<?>> map = new HashMap<>();

    /**
     * The getHandler() method is not supposed to be re-entrant.  This flag
     * is used to check that.
     *
     * Guarded by: this
     */
    private boolean inGetHandler = false;

    public B2JsonHandlerMap() {
        this(null);
    }

    /**
     * Handlers that need to be initialized.
     * <p>
     * Handlers are added when they are added to the map, and removed once a whole suite
     * of them has finished initialization.
     * <p>
     * Guarded by: this
     */
    private final List<B2JsonTypeHandler> handlersAddedToMap = new ArrayList<>();

    /**
     * Sets up a new map.
     */
    private B2JsonHandlerMap(Map<Class<?>, B2JsonTypeHandler<?>> initialMapOrNull) {
        // add all built-in handlers.
        map.put(BigDecimal.class, new B2JsonBigDecimalHandler());
        map.put(BigInteger.class, new B2JsonBigIntegerHandler());
        map.put(boolean.class, new B2JsonBooleanHandler(true));
        map.put(Boolean.class, new B2JsonBooleanHandler(false));
        map.put(byte.class, new B2JsonByteHandler(true));
        map.put(Byte.class, new B2JsonByteHandler(false));
        map.put(char.class, new B2JsonCharacterHandler(true));
        map.put(Character.class, new B2JsonCharacterHandler(false));
        map.put(int.class, new B2JsonIntegerHandler(true));
        map.put(Integer.class, new B2JsonIntegerHandler(false));
        map.put(LocalDate.class, new B2JsonLocalDateHandler());
        map.put(LocalDateTime.class, new B2JsonLocalDateTimeHandler());
        map.put(Duration.class, new B2JsonDurationHandler());
        map.put(long.class, new B2JsonLongHandler(true));
        map.put(Long.class, new B2JsonLongHandler(false));
        map.put(float.class, new B2JsonFloatHandler(true));
        map.put(Float.class, new B2JsonFloatHandler(false));
        map.put(double.class, new B2JsonDoubleHandler(true));
        map.put(Double.class, new B2JsonDoubleHandler(false));
        map.put(String.class, new B2JsonStringHandler());
        map.put(CharSequence.class, new B2JsonCharSquenceHandler());
        map.put(boolean[].class, new B2JsonBooleanArrayHandler(map.get(boolean.class)));
        map.put(char[].class, new B2JsonCharArrayHandler(map.get(char.class)));
        map.put(byte[].class, new B2JsonByteArrayHandler(map.get(byte.class)));
        map.put(int[].class, new B2JsonIntArrayHandler(map.get(int.class)));
        map.put(long[].class, new B2JsonLongArrayHandler(map.get(long.class)));
        map.put(float[].class, new B2JsonFloatArrayHandler(map.get(float.class)));
        map.put(double[].class, new B2JsonDoubleArrayHandler(map.get(double.class)));

        if (initialMapOrNull != null) {
            map.putAll(initialMapOrNull);
        }
    }

    /**
     * Gets the handler for a given class at the top level.
     * <p>
     * This method is called when it's time to (de)serialize some JSON, from the
     * toJson() and fromJson() methods.  It is not called by other handlers.  When
     * one handler depends on another, it should call the getUninitializedHandler()
     * method from its own initialize() method.
     * <p>
     * So, this method does NOT need to be re-entrant, and in fact we assume that it's not.
     */
    public synchronized <T> B2JsonTypeHandler<T> getHandler(Class<T> clazz) throws B2JsonException {

        // This method is NOT re-entrant.  The code that creates and initializes new handlers
        // should not call this method.
        B2Preconditions.checkState(!inGetHandler);
        B2Preconditions.checkState(handlersAddedToMap.isEmpty());

        // Fast path (without try/catch/finally) for the case where the handler is already in the map.
        {
            final B2JsonTypeHandler<T> existingHandlerOrNull = lookupHandler(clazz);
            if (existingHandlerOrNull != null) {
                return existingHandlerOrNull;
            }
        }

        inGetHandler = true;
        try {
            // Create any handlers that need to be created.
            B2JsonTypeHandler<T> handler = getUninitializedHandler(clazz);

            // Initialize handlers that were created.  Note that initializing a new handler
            // may result in new handlers being added to handlersAddedToMap, so this loop
            // needs to be able to handle the list changing as the loop progresses, so we
            // use an explicit index variable.
            //
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < handlersAddedToMap.size(); i++) {
                final B2JsonTypeHandler handlerAdded = handlersAddedToMap.get(i);
                if (handlerAdded instanceof B2JsonInitializedTypeHandler) {
                    ((B2JsonInitializedTypeHandler) handlerAdded).initialize(this);
                }
            }

            // Now that they're all initialized, all the types they depend on are ready to use,
            // so it's time to check the default values.
            for (B2JsonTypeHandler handlerAdded : handlersAddedToMap) {
                if (handlerAdded instanceof B2JsonInitializedTypeHandler) {
                    ((B2JsonInitializedTypeHandler) handlerAdded).checkDefaultValues();
                }
            }

            // Return the handler we found.
            return handler;
        }
        catch (Throwable t) {
            // Something went wrong, and the handlers are not ready to use, so we'll take them
            // out of the map.
            for (B2JsonTypeHandler handlerAdded : handlersAddedToMap) {
                map.remove(handlerAdded.getHandledClass());
            }

            // Let the caller know that something went wrong.
            throw new B2JsonException(t.getMessage());
        }
        finally {
            // Always clear the list of handlers that were added.
            handlersAddedToMap.clear();

            // And we're no longer in this method.
            B2Preconditions.checkState(inGetHandler);
            inGetHandler = false;
        }
    }

    /**
     * Gets the handler for a given class at the top level.
     *
     * The handler MAY NOT BE INITIALIZED.  This method is for use by handlers that need to get
     * a reference to another handler in their initialize() methods.  You cannot assume that any
     * fields set by initialize() have been set.
     */
    /*package*/ synchronized <T> B2JsonTypeHandler<T> getUninitializedHandler(Class<T> clazz) throws B2JsonException {

        B2JsonTypeHandler<T> result = lookupHandler(clazz);

        if (result == null) {
            // maybe use a custom handler provided by clazz.
            result = findCustomHandler(clazz);
            if (result != null) {
                rememberHandler(clazz, result);
            }
        }

        if (result == null) {
            if (clazz.isEnum()) {
                result = new B2JsonEnumHandler<>(clazz);
                rememberHandler(clazz, result);
            } else if (clazz.isArray()) {
                final Class eltClazz = clazz.getComponentType();
                //noinspection unchecked
                B2JsonTypeHandler eltClazzHandler = getUninitializedHandler(eltClazz);
                //noinspection unchecked
                result = (B2JsonTypeHandler<T>) new B2JsonObjectArrayHandler(clazz, eltClazz, eltClazzHandler);
                rememberHandler(clazz, result);
            } else if (isUnionBase(clazz)) {
                //noinspection unchecked
                result = (B2JsonTypeHandler<T>) new B2JsonUnionBaseHandler(clazz);
                rememberHandler(clazz, result);
            } else {
                //noinspection unchecked
                result = (B2JsonTypeHandler<T>) new B2JsonObjectHandler(clazz);
                rememberHandler(clazz, result);
            }
        }

        return result;
    }

    /**
     * Returns the type for a field in an object.
     *
     * This handles types, such as Set<>, that are not supported as top-level objects by B2Json.
     *
     * The handler MAY NOT BE INITIALIZED.  This method is for use by handlers that need to get
     * a reference to another handler in their initialize() methods.  You cannot assume that any
     * fields set by initialize() have been set.
     */
    /*package*/ static B2JsonTypeHandler getUninitializedFieldHandler(Type fieldType, B2JsonHandlerMap handlerMap) throws B2JsonException {
        if (fieldType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) fieldType;
            final Class rawType = (Class) paramType.getRawType();
            if (rawType == LinkedHashSet.class) {
                Type itemType = paramType.getActualTypeArguments()[0];
                B2JsonTypeHandler<?> itemHandler = getUninitializedFieldHandler(itemType, handlerMap);
                return new B2JsonLinkedHashSetHandler(itemHandler);
            }
            if (rawType == List.class) {
                Type itemType = paramType.getActualTypeArguments()[0];
                B2JsonTypeHandler<?> itemHandler = getUninitializedFieldHandler(itemType, handlerMap);
                return new B2JsonListHandler(itemHandler);
            }
            if (rawType == TreeSet.class) {
                Type itemType = paramType.getActualTypeArguments()[0];
                B2JsonTypeHandler<?> itemHandler = getUninitializedFieldHandler(itemType, handlerMap);
                return new B2JsonTreeSetHandler(itemHandler);
            }
            if (rawType == Set.class) {
                Type itemType = paramType.getActualTypeArguments()[0];
                B2JsonTypeHandler<?> itemHandler = getUninitializedFieldHandler(itemType, handlerMap);
                return new B2JsonSetHandler(itemHandler);
            }
            if (rawType == EnumSet.class) {
                Type itemType = paramType.getActualTypeArguments()[0];
                B2JsonTypeHandler<?> itemHandler = getUninitializedFieldHandler(itemType, handlerMap);
                return new B2JsonEnumSetHandler(itemHandler);
            }
            if (rawType == Map.class || rawType == TreeMap.class) {
                Type keyType = paramType.getActualTypeArguments()[0];
                Type valueType = paramType.getActualTypeArguments()[1];
                B2JsonTypeHandler<?> keyHandler = getUninitializedFieldHandler(keyType, handlerMap);
                B2JsonTypeHandler<?> valueHandler = getUninitializedFieldHandler(valueType, handlerMap);
                return new B2JsonMapHandler(keyHandler, valueHandler);
            }
            if (rawType == ConcurrentMap.class) {
                Type keyType = paramType.getActualTypeArguments()[0];
                Type valueType = paramType.getActualTypeArguments()[1];
                B2JsonTypeHandler<?> keyHandler = getUninitializedFieldHandler(keyType, handlerMap);
                B2JsonTypeHandler<?> valueHandler = getUninitializedFieldHandler(valueType, handlerMap);
                return new B2JsonConcurrentMapHandler(keyHandler, valueHandler);
            }
        }
        if (fieldType instanceof Class) {
            final Class fieldClass = (Class) fieldType;
            //noinspection unchecked
            return handlerMap.getUninitializedHandler(fieldClass);
        }
        throw new B2JsonException("Do not know how to handle: " + fieldType);
    }

    /**
     * Returns a list of all of the fields in a class that should be included in JSON.
     */
    /*package*/ static List<Field> getObjectFieldsForJson(Class<?> clazz) throws B2JsonException {
        final List<Field> result = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            FieldInfo.FieldRequirement requirement = getFieldRequirement(clazz, field);
            if (!Modifier.isStatic(field.getModifiers()) && requirement != FieldInfo.FieldRequirement.IGNORED) {
                result.add(field);
            }
        }
        return result;
    }

    /**
     * Returns the ignored/optional/required/ignored status of a field in a class.
     */
    /*package*/ static FieldInfo.FieldRequirement getFieldRequirement(Class<?> clazz, Field field) throws B2JsonException {

        // We never handle static fields
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            return FieldInfo.FieldRequirement.IGNORED;
        }

        // Get the annotation to see how we should handle it.
        FieldInfo.FieldRequirement result = null;
        int count = 0;
        if (field.getAnnotation(B2Json.required.class) != null) {
            result = FieldInfo.FieldRequirement.REQUIRED;
            count += 1;
        }
        if (field.getAnnotation(B2Json.optional.class) != null) {
            result = FieldInfo.FieldRequirement.OPTIONAL;
            count += 1;
        }
        if (field.getAnnotation(B2Json.optionalWithDefault.class) != null) {
            result = FieldInfo.FieldRequirement.OPTIONAL;
            count += 1;
        }
        if (field.getAnnotation(B2Json.ignored.class) != null) {
            result = FieldInfo.FieldRequirement.IGNORED;
            count += 1;
        }
        if (count != 1) {
            throw new B2JsonException(clazz.getName() + "." + field.getName() + " should have exactly one annotation: required, optional, optionalWithDefault, or ignored");
        }
        return result;
    }

    /**
     * Is this class the base class for a union type?
     *
     * Union bases have the @union annotation.
     */
    /*package*/ static <T> boolean isUnionBase(Class<T> clazz) {
        return clazz.getAnnotation(B2Json.union.class) != null;
    }

    private <T> B2JsonTypeHandler<T> findCustomHandler(Class<T> clazz) throws B2JsonException {
        // this does NOT need to be synchronized because it doesn't touch the map.

        // i'm using getDeclaredMethod instead of just getMethod so that classes
        // can't inherit the type handler from their superclass.  that seems like
        // a safer starting point.
        Method method = null;
        try {
            method = clazz.getDeclaredMethod("getJsonTypeHandler");
            method.setAccessible(true);
            final Object obj = method.invoke(null);
            if (obj instanceof B2JsonTypeHandler) {
                //noinspection unchecked
                return (B2JsonTypeHandler<T>) obj;
            } else {
                String objType = (obj == null) ? "null" : obj.getClass().getName();
                throw new B2JsonException(clazz.getSimpleName() + "." + method.getName() + "() returned an unexpected type of object (" + objType + ")");
            }
        } catch (NoSuchMethodException e) {
            // this class just didn't declare a handler.  oh well.
            return null;
        } catch (InvocationTargetException e) {
            throw new B2JsonException("failed to invoke " + method + ": " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new B2JsonException("illegal access to " + method + ": " + e.getMessage(), e);
        }
    }

    private synchronized <T> B2JsonTypeHandler<T> lookupHandler(Class<T> clazz) {
        // this is a method to make it easy to synchronize it.  it's private, so
        // i'm hoping the compiler considers inlining it.
        //noinspection unchecked
        return (B2JsonTypeHandler<T>) map.get(clazz);
    }

    /**
     * Saves a handler in the map, remembering to use it for the given class.
     *
     * This method is not private because it is needed by B2JsonObjectHandler,
     * so that it can store itself in the map before trying to make handlers
     * for its fields, which may be recursive and be of the same type.  When
     * this happens, the handler stored IS NOT READY YET, because its constructor
     * is not done yet.  This is safe because it all happens within a call
     * to B2JsonHandlerMap.getHandler(), which synchronized and keeps anybody
     * else from seeing the B2JsonObjectHandler before it is fully constructed.
     */
    private synchronized <T> void rememberHandler(Class<T> clazz, B2JsonTypeHandler<T> handler) {
        B2Preconditions.checkState(!map.containsKey(clazz));
        map.put(clazz, handler);
        handlersAddedToMap.add(handler);
    }
}
