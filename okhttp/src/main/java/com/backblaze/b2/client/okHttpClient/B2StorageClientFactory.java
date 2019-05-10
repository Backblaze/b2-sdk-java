package com.backblaze.b2.client.okHttpClient;

import com.backblaze.b2.client.B2StorageClient;

public class B2StorageClientFactory {
    public static B2StorageClient make(String userAgent){
        return B2StorageOkHttpClientBuilder.builder(userAgent).build();
    }
    public static B2StorageClient make2(String userAgent){
        return make(userAgent);
    }
}