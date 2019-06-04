/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.util.B2Preconditions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * This implementation of B2StorageClientFactory has a list of registered class names
 * and instantiates the first one of those classes which can be loaded.
 *
 * Instances of this class start out knowing about the class names for all of the
 * official implementations.  If it makes sense to let code that uses the library
 * register implementations, we can make registerClass() public, but it's possible
 * that it'll be easier for them to just make their own factory entirely.
 *
 *
 * THREAD-SAFE.
 */
public class B2StorageClientFactoryPathBasedImpl implements B2StorageClientFactory {
    /**
     * A list of class names.  Some or most of them might not be loadable,
     * but, if they are loadable, they must implement B2StorageClientFactory.
     *
     * THREAD-SAFETY: Synchronize on the instance before reading or writing this variable.
     */
    private List<String> factoryClassNames = new ArrayList<>();

    /**
     * This is either null or the factory we've found and created using the the factoryClassNames.
     * Note that once we've picked one, we stick with it, even if it fails.
     *
     * THREAD-SAFETY: Synchronize on the instance before reading or writing this variable.
     */
    private B2StorageClientFactory factory;

    B2StorageClientFactoryPathBasedImpl() {
        // register the Apache HttpClient-based implementation:
        registerClass("com.backblaze.b2.client.webApiHttpClient.B2StorageHttpClientFactory");

        // register the okhttp-based implementation:
        registerClass("com.backblaze.b2.client.okHttpClient.B2StorageOkHttpClientFactory");

        // register the UrlConnection-based implementation:
        registerClass("com.backblaze.b2.client.webApiUrlConnectionClient.B2WebApiUrlConnectionClientFactory");

    }

    /**
     * @param className the fully-qualified name of a class that implements B2StorageClientFactory and
     *                  has a no-arguments constructor.
     */
    /*forTests*/ synchronized void registerClass(String className) {
        B2Preconditions.checkArgument(!factoryClassNames.contains(className), className + " was already registered?");

        // adding to the front of the list so that tests can add their own implementations before the built in ones.
        factoryClassNames.add(0, className);
    }

    /**
     * @param config the configuration to use.
     * @return a new B2StorageClient or throws a RuntimeException if it can't make one.
     */
    public synchronized B2StorageClient create(B2ClientConfig config) {
        if (factory == null) {
            factory = findAndCreateFactory();
        }

        return factory.create(config);
    }

    /**
     * @return the createDefault() method from the first class in storageClientClassNames.
     */
    private synchronized B2StorageClientFactory findAndCreateFactory() {
        for (String className : factoryClassNames) {
            try {
                final Class<?> clazz = Class.forName(className);
                final Constructor<?> ctor = clazz.getConstructor();
                final Object factory = ctor.newInstance();
                if (!(factory instanceof B2StorageClientFactory)) {
                    throw new RuntimeException(className + " doesn't implement B2StorageClientFactory.");
                }
                return (B2StorageClientFactory) factory;
            } catch (ClassNotFoundException e) {
                // this is normal.  we usually have entries in the list that aren't in the class path.
            } catch (NoSuchMethodException e) {
                // this is a bit surprising.  the class exists, but doesn't have a no-parameter constructor.
                // that seems like a coding problem in a jar on the path.  go ahead and throw to
                // help the developer who is working on that class.
                throw new RuntimeException("class " + className + " doesn't have a public no-argument constructor?", e);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException("unable to instantiate " + className + ": " + e, e);
            }
        }

        throw new RuntimeException("can't find any of the registered classes.");
    }
}
