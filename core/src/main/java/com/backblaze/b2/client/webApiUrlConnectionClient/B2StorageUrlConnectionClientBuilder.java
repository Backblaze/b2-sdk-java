/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiUrlConnectionClient;

import com.backblaze.b2.client.B2ClientConfig;
import com.backblaze.b2.client.B2DefaultRetryPolicy;
import com.backblaze.b2.client.B2RetryPolicy;
import com.backblaze.b2.client.B2Sdk;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientImpl;
import com.backblaze.b2.client.B2StorageClientWebifier;
import com.backblaze.b2.client.B2StorageClientWebifierImpl;
import com.backblaze.b2.client.webApiClients.B2WebApiClient;

import java.util.function.Supplier;

public class B2StorageUrlConnectionClientBuilder {
    private static final String DEFAULT_MASTER_URL = "https://api.backblazeb2.com/";
    private final B2ClientConfig config;
    private B2WebApiClient webApiClient;
    private Supplier<B2RetryPolicy> retryPolicySupplier;

    @SuppressWarnings("WeakerAccess")
    public static B2StorageUrlConnectionClientBuilder builder(B2ClientConfig config) {
        return new B2StorageUrlConnectionClientBuilder(config);
    }

    private B2StorageUrlConnectionClientBuilder(B2ClientConfig config) {
        this.config = config;
    }

    public B2StorageClient build() {
        final B2WebApiClient webApiClient = (this.webApiClient != null) ?
                this.webApiClient :
                new B2WebApiUrlConnectionClientImpl();
        final B2StorageClientWebifier webifier = new B2StorageClientWebifierImpl(
                webApiClient,
                config.getUserAgent() + " " + B2Sdk.getName() + "/" + B2Sdk.getVersion(),
                (config.getMasterUrl() == null) ? DEFAULT_MASTER_URL : config.getMasterUrl(),
                config.getTestModeOrNull());
        final Supplier<B2RetryPolicy> retryPolicySupplier = (this.retryPolicySupplier != null) ?
                this.retryPolicySupplier :
                B2DefaultRetryPolicy.supplier();
        return new B2StorageClientImpl(
                webifier,
                config,
                retryPolicySupplier);
    }

    @SuppressWarnings("unused")
    public B2StorageUrlConnectionClientBuilder setRetryPolicySupplier(Supplier<B2RetryPolicy> retryPolicySupplier) {
        this.retryPolicySupplier = retryPolicySupplier;
        return this;
    }
}
