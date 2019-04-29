/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.okHttpClient;

import com.backblaze.b2.client.B2AccountAuthorizer;
import com.backblaze.b2.client.B2AccountAuthorizerSimpleImpl;
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

public class B2StorageOkHttpClientBuilder {

    private static final String PRODUCTION_MASTER_URL = "https://api.backblazeb2.com/";
    private static final String STAGING_MASTER_URL = "https://api.backblazeb2.net/";
    private final B2ClientConfig config;
    private Supplier<B2RetryPolicy> retryPolicySupplier;
    private B2OkHttpClientImpl.ProgressListener progressListener;

    @SuppressWarnings("WeakerAccess")
    public static B2StorageOkHttpClientBuilder builder(B2ClientConfig config) {
        return new B2StorageOkHttpClientBuilder(config);
    }

    @SuppressWarnings("WeakerAccess")
    public static B2StorageOkHttpClientBuilder builder(String accountId, String applicationKey, String userAgent) {
        final B2AccountAuthorizer accountAuthorizer = B2AccountAuthorizerSimpleImpl
                .builder(accountId, applicationKey)
                .build();
        final B2ClientConfig config = B2ClientConfig
                .builder(accountAuthorizer, userAgent)
                .build();
        return builder(config);
    }

    public B2StorageOkHttpClientBuilder progressListener(B2OkHttpClientImpl.ProgressListener progressListener){
        this.progressListener = progressListener;
        return this;
    }
    private B2StorageOkHttpClientBuilder(B2ClientConfig config) {
        this.config = config;
    }

    public B2StorageClient build() {
        B2OkHttpClientImpl clientImpl = new B2OkHttpClientImpl();
        if( progressListener != null) {
            clientImpl.setProgressListener(progressListener);
        }
        final B2WebApiClient webApiClient = clientImpl;
        final B2StorageClientWebifier webifier = new B2StorageClientWebifierImpl(
                webApiClient,
                config.getUserAgent() + " " + B2Sdk.getName() + "/" + B2Sdk.getVersion(),
                (config.getMasterUrl() == null) ? PRODUCTION_MASTER_URL : config.getMasterUrl(),
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
    public B2StorageOkHttpClientBuilder setRetryPolicySupplier(Supplier<B2RetryPolicy> retryPolicySupplier) {
        this.retryPolicySupplier = retryPolicySupplier;
        return this;
    }
}
