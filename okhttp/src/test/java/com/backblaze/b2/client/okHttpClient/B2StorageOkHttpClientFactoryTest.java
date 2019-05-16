/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.okHttpClient;

import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.B2StorageClientFactoryPathBasedImpl;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class B2StorageOkHttpClientFactoryTest {

    @Test
    public void testCreate() {
        // this is mostly to keep B2StorageHttpClientFactory from being unused.
        final B2StorageOkHttpClientFactory factory = new B2StorageOkHttpClientFactory();
        assertNotNull(factory);
    }

    @Test
    public void testDefaultFactory_succeedsBecauseTestEnvironmentIncludesHttpClientJars() {
        final B2StorageClientFactory factory = B2StorageClientFactory.createDefaultFactory();
        assertTrue(factory instanceof B2StorageClientFactoryPathBasedImpl);

        assertNotNull(factory.create("appKeyId", "appKey", "userAgent"));
    }
}
