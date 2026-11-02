/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import com.backblaze.b2.client.exceptions.B2Exception;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;

public interface HttpClientFactory extends AutoCloseable {

    /**
     * This returns a CloseableHttpClient (instead of an HttpClient) because
     * the SDK needs to be able to close the responses to allow connections
     * to be reused.
     *
     * Note that even though this returns a CloseableHttpClient,
     * the SDK will *not* call close() on it, because doing so
     * would close the client's HttpClientConnectionManager.
     *
     * @return a new httpClient for use by the SDK.
     *         this will be called often.
     * @throws B2Exception if there's any trouble creating the client.
     */
    CloseableHttpClient create() throws B2Exception;

    /**
     * @return the RequestConfig (timeouts, etc.) the clients from create() use by default.
     *         The SDK copies this for downloads, so custom factories should override it
     *         if they configure non-default timeouts.
     */
    default RequestConfig getRequestConfig() {
        return HttpClientFactoryImpl.builder().createRequestConfig();
    }

    /**
     * Called to release resources, such as an HttpClientConnectionManager.
     */
    @Override
    void close();
}
