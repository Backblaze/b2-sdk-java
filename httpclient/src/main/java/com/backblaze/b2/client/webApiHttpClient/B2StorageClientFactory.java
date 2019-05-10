package com.backblaze.b2.client.webApiHttpClient;

import com.backblaze.b2.client.B2StorageClient;

public class B2StorageClientFactory {
    public static B2StorageClient make(String userAgent){
        return B2StorageHttpClientBuilder.builder(userAgent).build();
    }

    public static B2StorageClient make2(String userAgent){
        final HttpClientFactory httpClientFactory = HttpClientFactoryImpl
                .builder()
                .build();
        return B2StorageHttpClientBuilder
                .builder(userAgent)
                .setHttpClientFactory(httpClientFactory)
                .build();
    }
}
