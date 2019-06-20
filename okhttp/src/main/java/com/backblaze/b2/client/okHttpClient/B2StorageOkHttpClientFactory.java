/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.okHttpClient;

import com.backblaze.b2.client.B2ClientConfig;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;

/**
 * Simple factory for the OkHttp-based StorageClient.
 *
 * THREAD-SAFE.
 */

public class B2StorageOkHttpClientFactory implements B2StorageClientFactory {

    @Override
    public B2StorageClient create(B2ClientConfig config) {
        return B2StorageOkHttpClientBuilder.builder(config).build();
    }
}
