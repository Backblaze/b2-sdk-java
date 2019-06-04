/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiClients;

import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.B2StorageClientFactoryPathBasedImpl;
import com.backblaze.b2.client.webApiUrlConnectionClient.B2WebApiUrlConnectionClientFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class B2WebApiUrlConnectionClientFactoryTest {
    @Test
    public void testCreate() {
        // this is mostly to keep B2StorageHttpClientFactory from being unused.
        final B2WebApiUrlConnectionClientFactory factory = new B2WebApiUrlConnectionClientFactory();
        assertNotNull(factory);
    }

    @Test
    public void testDefaultFactory_succeedsBecauseTestEnvironmentIncludesUrlConnectionBasedImplementation() {
        final B2StorageClientFactory factory = B2StorageClientFactory.createDefaultFactory();
        assertTrue(factory instanceof B2StorageClientFactoryPathBasedImpl);

        assertNotNull(factory.create("appKeyId", "appKey", "userAgent"));
    }
}