/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

public class B2StorageClientFactoryPathBasedImplTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testDefaultFactory_failsSucceedsTestEnvironmentIncludesUrlBasedImplementation() {
        final B2StorageClientFactory factory = B2StorageClientFactory.createDefaultFactory();

        assertNotNull(factory.create("appKeyId", "appKey", "userAgent"));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static class FactoryThatMakesMocks implements B2StorageClientFactory {
        @Override
        public B2StorageClient create(B2ClientConfig config) {
            return mock(B2StorageClient.class);
        }
    }

    @Test
    public void testRegisterClassWorks() {
        // register the class above.
        final B2StorageClientFactoryPathBasedImpl factory = new B2StorageClientFactoryPathBasedImpl();
        factory.registerClass(FactoryThatMakesMocks.class.getName());

        // use the factory.
        final B2StorageClient client = factory.create("appKeyId", "appKey", "userAgent");
        assertTrue(mockingDetails(client).isMock());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unused") // used by reflection
    public static class FactoryWithoutRequiredConstructor implements B2StorageClientFactory {
        private FactoryWithoutRequiredConstructor() {
        }

        @Override
        public B2StorageClient create(B2ClientConfig config) {
            return null;
        }
    }

    @Test
    public void testRegisteringClassWithoutCorrectConstructor() {
        final String className = FactoryWithoutRequiredConstructor.class.getName();
        checkThrows(className,"class " + className + " doesn't have a public no-argument constructor?");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings({"unused", "WeakerAccess"}) // used by reflection
    public static class FactoryWithoutRequiredInterface {
    }

    @Test
    public void testRegisteringClassWhichDoesntImplementCorrectInterface() {
        final String className = FactoryWithoutRequiredInterface.class.getName();
        checkThrows(className,className + " doesn't implement B2StorageClientFactory.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unused") // used by reflection.
    public static class FactoryThatThrowsInConstructor implements B2StorageClientFactory {

        public FactoryThatThrowsInConstructor() {
            throw new RuntimeException("throws in constructor");
        }

        @Override
        public B2StorageClient create(B2ClientConfig config) {
            return mock(B2StorageClient.class);
        }
    }

    @Test
    public void testRegisteringClassWhichThrowsInConstructor() {
        final String className = FactoryThatThrowsInConstructor.class.getName();
        checkThrows(className,"unable to instantiate " + className + ": java.lang.reflect.InvocationTargetException");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void checkThrows(String className, String expectedMessage) {
        final B2StorageClientFactoryPathBasedImpl factory = new B2StorageClientFactoryPathBasedImpl();
        factory.registerClass(className);

        thrown.expectMessage(expectedMessage);
        factory.create("appKeyId", "appKey", "userAgent");
    }
}