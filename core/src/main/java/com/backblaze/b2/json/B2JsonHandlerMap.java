/*
 * Copyright 2023, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.json;

import com.backblaze.b2.util.B2Preconditions;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Holds a mapping from Class to B2JsonTypeHandler.
 * <p>
 * The mapping starts out with initial contents, which must be ALL
 * of the non-default mappings that will be used.  If any other
 * handlers are needed, the default B2JsonObjectHandler will be used
 * for that class.
 * <p>
 * This class is THREAD SAFE.
 */
public class B2JsonHandlerMap {

    // access to this map is always synchronized on this object.
    // we think it's safe to overwrite the entry for a given class because
    // we assume all handlers are stateless and any two instances of
    // a handler for a class are equivalent.
    private final Map<Type, B2JsonTypeHandler<?>> map = new HashMap<>();

    /**
     * All Handlers that are ready to use without any further work needed.
     * For example, B2JsonInitializedTypeHandler handlers have three phases according to the Javadoc: construction,
     * initialization and validation of default values.  These types of handlers could be included in the
     * map variable before all three phases are complete, which would be problematic for the lock
     * contention optimization that does not synchronize if the handler exists in the map.  Instead,
     * a separate mapWithHandlersReadyToUse variable is created where the entries that exist are sure to need
     * no further initialization work performed.
     */
    private final Map<Type, B2JsonTypeHandler<?>> mapWithHandlersReadyToUse = new ConcurrentHashMap<>(200);

    /**
     * The getHandler() method is not supposed to be re-entrant.  This flag
     * is used to check that.
     * <p>
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
    private B2JsonHandlerMap(Map<Type, B2JsonTypeHandler<?>> initialMapOrNull) {
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
        map.put(AtomicLongArray.class, new B2JsonAtomicLongArrayHandler(new B2JsonLongHandler(false)));

        if (initialMapOrNull != null) {
            initialMapOrNull.forEach(this::putHandler);
        }

        mapWithHandlersReadyToUse.putAll(map);
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
    public <T> B2JsonTypeHandler<T> getHandler(Type type) throws B2JsonException {
        // Fast path (without try/catch/finally) for the case where the handler is already
        // in the mapWithHandlersReadyToUse.  Don't synchronize on the fast path
        {
            final B2JsonTypeHandler<T> existingHandlerOrNullReadyToUse =
                    (B2JsonTypeHandler<T>) mapWithHandlersReadyToUse.get(type);
            if (existingHandlerOrNullReadyToUse != null) {
                    return existingHandlerOrNullReadyToUse;
            }
        }

        synchronized (this) {
            // This method is NOT re-entrant.  The code that creates and initializes new handlers
            // should not call this method.
            //
            // The reason this code cannot be re-entrant is that it would try to create more
            // new handlers, which could make it impossible for this method to un-do what it
            // has done by removing the handlers it had added.  A re-entrant call could
            // wind up creating dependencies on the classes we temporarily added but wound
            // up removing.
            B2Preconditions.checkState(!inGetHandler);
            B2Preconditions.checkState(handlersAddedToMap.isEmpty());

            {
                final B2JsonTypeHandler<T> existingHandlerOrNull = lookupHandler(type);
                if (existingHandlerOrNull != null) {
                    return existingHandlerOrNull;
                }
            }

            inGetHandler = true;
            final B2JsonTypeHandler<T> handler;
            final List<B2JsonTypeHandler> handlersToCheckDefaults;
            try {
                // Create any handlers that need to be created.
                handler = getUninitializedHandler(type);

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

                // NOTE: It is not possible to run the default value checks at this point.
                // Up until this point we have not had to initialize any of the classes
                // whose handlers were created and initialized.  (Reflection to see fields
                // and annotations does not require initializing the class.)
                //
                // If we were to check default values, that would create instances, which
                // would first require initializing the classes.  And classes can have
                // arbitrary code in their static initializers, which could call B2Json.
                // Those calls to B2Json could wind up calling getHandler() on more
                // classes, thus violating the no-re-entry precondition, and they could
                // wind up trying to use the handlers we're in the process of setting up.

                // Remember the handlers we added so we can check their defaults.
                handlersToCheckDefaults = new ArrayList<>(handlersAddedToMap);
            } catch (Throwable t) {
                // Something went wrong, and the handlers are not ready to use, so we'll take them
                // out of the map.
                for (B2JsonTypeHandler handlerAdded : handlersAddedToMap) {
                    map.remove(handlerAdded.getHandledType());
                }

                // Let the caller know that something went wrong.
                throw new B2JsonException(t.getMessage());
            } finally {
                // Always clear the list of handlers that were added.
                handlersAddedToMap.clear();

                // And we're no longer in this method.
                B2Preconditions.checkState(inGetHandler);
                inGetHandler = false;
            }

            // Now we can check default values.
            //
            // Note that we have already committed to keeping the handlers we created,
            // but we can still mark them as having bad defaults.
            //
            // This leaves an interval now where other threads could use the handlers, but
            // might not get told that they are unusable because of bad default values.
            // What we do guarantee is that the first caller to need the handler (this thread)
            // will get an error, and that any thread that calls after this method returns
            // will get an error.
            try {
                for (B2JsonTypeHandler<?> handlerToCheck : handlersToCheckDefaults) {
                    if (handlerToCheck instanceof B2JsonTypeHandlerWithDefaults) {
                        final B2JsonTypeHandlerWithDefaults handlerWithDefaults =
                                (B2JsonTypeHandlerWithDefaults) handlerToCheck;
                        handlerWithDefaults.checkDefaultValuesAndRememberResult();
                    }
                }
            } finally {
                handlersAddedToMap.clear();
            }

            // All done.
            mapWithHandlersReadyToUse.put(handler.getHandledType(), handler);
            return handler;
        }
    }

    /**
     * Gets the handler for a given type at the top level.
     * <p>
     * The type must be resolved. If the type represents a generic class, the type must also
     * have concrete type arguments. Otherwise this will fail.
     * <p>
     * The handler MAY NOT BE INITIALIZED.  This method is for use by handlers that need to get
     * a reference to another handler in their initialize() methods.  You cannot assume that any
     * fields set by initialize() have been set.
     */
    /*package*/ synchronized <T> B2JsonTypeHandler<T> getUninitializedHandler(Type type) throws B2JsonException {
        // We do not need to check if the type is resolved here. That will happen as we recurse. If we come across
        // a field that cannot be resolved, we will throw then.

        // Check to see if we've already done the work for this type.
        {
            final B2JsonTypeHandler<T> handler = lookupHandler(type);
            if (handler != null) {
                return handler;
            }
        }

        final B2JsonTypeHandler<T> handler;
        if (type instanceof Class) {
            // class Item {
            //     private int itemNumber;
            //     private String itemName;
            // }
            //noinspection unchecked
            final Class<T> clazz = (Class<T>) type;
            handler = getUninitializedHandlerForClass(clazz);

        } else if (type instanceof ParameterizedType) {
            // class Dataset {
            //     private List<Double> measurements;
            // }
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            //noinspection unchecked
            handler = getUninitializedHandlerForParameterizedType(parameterizedType);

        } else if (type instanceof GenericArrayType) {
            final GenericArrayType genericArrayType = (GenericArrayType) type;
            handler = getUninitializedHandlerForGenericArrayType(genericArrayType);

        } else {
            throw new B2JsonException("do not know how to get handler for type " + type.getTypeName());
        }
        rememberHandler(type, handler);
        return handler;
    }

    private synchronized <T> B2JsonTypeHandler<T> getUninitializedHandlerForClass(Class<T> clazz) throws B2JsonException {

        // maybe use a custom handler provided by clazz.
        B2JsonTypeHandler<T> result = findCustomHandler(clazz);
        if (result != null) {
            return result;
        }

        if (clazz.isEnum()) {
            return new B2JsonEnumHandler<>(clazz);
        }

        if (clazz.isArray()) {
            final Class eltClazz = clazz.getComponentType();
            B2JsonTypeHandler eltClazzHandler = getUninitializedHandler(eltClazz);
            //noinspection unchecked
            return (B2JsonTypeHandler<T>) new B2JsonObjectArrayHandler(clazz, eltClazz, eltClazzHandler);
        }

        if (hasUnionAnnotation(clazz)) {
            //noinspection unchecked
            return (B2JsonTypeHandler<T>) new B2JsonUnionBaseHandler(clazz);
        }

        //noinspection unchecked
        return (B2JsonTypeHandler<T>) new B2JsonObjectHandler(clazz);
    }

    private synchronized B2JsonTypeHandler getUninitializedHandlerForParameterizedType(
            ParameterizedType parameterizedType) throws B2JsonException {

        final Type rawType = parameterizedType.getRawType();
        if (rawType.equals(LinkedHashSet.class)) {
            Type itemType = parameterizedType.getActualTypeArguments()[0];
            B2JsonTypeHandler<?> itemHandler = getUninitializedHandler(itemType);
            return new B2JsonLinkedHashSetHandler(itemHandler);
        }
        if (rawType.equals(List.class)) {
            Type itemType = parameterizedType.getActualTypeArguments()[0];
            B2JsonTypeHandler<?> itemHandler = getUninitializedHandler(itemType);
            return new B2JsonListHandler(itemHandler);
        }
        if (rawType.equals(TreeSet.class)) {
            Type itemType = parameterizedType.getActualTypeArguments()[0];
            B2JsonTypeHandler<?> itemHandler = getUninitializedHandler(itemType);
            return new B2JsonTreeSetHandler(itemHandler);
        }
        if (rawType.equals(Set.class)) {
            Type itemType = parameterizedType.getActualTypeArguments()[0];
            B2JsonTypeHandler<?> itemHandler = getUninitializedHandler(itemType);
            return new B2JsonSetHandler(itemHandler);
        }
        if (rawType.equals(EnumSet.class)) {
            Type itemType = parameterizedType.getActualTypeArguments()[0];
            B2JsonTypeHandler<?> itemHandler = getUninitializedHandler(itemType);
            return new B2JsonEnumSetHandler(itemHandler);
        }
        if (rawType.equals(Map.class) || rawType.equals(SortedMap.class) || rawType.equals(TreeMap.class)) {
            Type keyType = parameterizedType.getActualTypeArguments()[0];
            Type valueType = parameterizedType.getActualTypeArguments()[1];
            B2JsonTypeHandler<?> keyHandler = getUninitializedHandler(keyType);
            B2JsonTypeHandler<?> valueHandler = getUninitializedHandler(valueType);
            return new B2JsonMapHandler(keyHandler, valueHandler);
        }
        if (rawType.equals(ConcurrentMap.class)) {
            Type keyType = parameterizedType.getActualTypeArguments()[0];
            Type valueType = parameterizedType.getActualTypeArguments()[1];
            B2JsonTypeHandler<?> keyHandler = getUninitializedHandler(keyType);
            B2JsonTypeHandler<?> valueHandler = getUninitializedHandler(valueType);
            return new B2JsonConcurrentMapHandler(keyHandler, valueHandler);
        }
        final Type resolvedRawType = parameterizedType.getRawType();
        // Not sure if the resolvedRawType can be anything other than a class, but if it's not it's a bug.
        B2Preconditions.checkArgument(resolvedRawType instanceof Class);
        final Class resolvedRawTypeClass = (Class) resolvedRawType;
        //noinspection unchecked
        return new B2JsonObjectHandler(resolvedRawTypeClass, parameterizedType.getActualTypeArguments());
    }

    private synchronized B2JsonTypeHandler getUninitializedHandlerForGenericArrayType(
            GenericArrayType genericArrayType) throws B2JsonException {

        // Java does not allow the component type to be a parameterized type. Therefore,
        // we can get the Class of the generic component Type without losing information.
        return new B2JsonObjectArrayHandler(
                genericArrayType.getClass(),
                genericArrayType.getGenericComponentType().getClass(),
                getUninitializedHandler(genericArrayType.getGenericComponentType()));
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
     * Does this class have the @B2Json.union annotation?
     */
    /*package*/ static <T> boolean hasUnionAnnotation(Class<T> clazz) {
        return clazz.getAnnotation(B2Json.union.class) != null;
    }

    private <T> B2JsonTypeHandler<T> findCustomHandler(Class<T> clazz) throws B2JsonException {
        // this does NOT need to be synchronized because it doesn't touch the map.

        // i'm using getDeclaredMethod instead of just getMethod so that classes
        // can't inherit the type handler from their superclass.  that seems like
        // a safer starting point.
        Method method = null;
        try {
            // Calling the getJsonTypeHandler method will cause the class initializer
            // to run, which could call B2Json, which could cause problems.
            // See https://github.com/Backblaze/b2-sdk-java/issues/88
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

    private synchronized <T> B2JsonTypeHandler<T> lookupHandler(Type type) {
        // this is a method to make it easy to synchronize it.  it's private, so
        // i'm hoping the compiler considers inlining it.
        //noinspection unchecked
        return (B2JsonTypeHandler<T>) map.get(type);
    }

    /**
     * Saves a handler in the map, remembering to use it for the given class.
     * <p>
     * This method is not private because it is needed by B2JsonObjectHandler,
     * so that it can store itself in the map before trying to make handlers
     * for its fields, which may be recursive and be of the same type.  When
     * this happens, the handler stored IS NOT READY YET, because its constructor
     * is not done yet.  This is safe because it all happens within a call
     * to B2JsonHandlerMap.getHandler(), which synchronized and keeps anybody
     * else from seeing the B2JsonObjectHandler before it is fully constructed.
     */
    private synchronized <T> void rememberHandler(Type type, B2JsonTypeHandler<T> handler) {
        B2Preconditions.checkState(!map.containsKey(type));
        putHandler(type, handler);
        handlersAddedToMap.add(handler);
    }

    private synchronized <T> void putHandler(Type type, B2JsonTypeHandler<T> handler) {
        // TODO validation?
        map.put(type, handler);
    }
}
