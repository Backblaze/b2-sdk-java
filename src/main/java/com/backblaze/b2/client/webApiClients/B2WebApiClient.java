/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiClients;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;

import java.io.InputStream;

public interface B2WebApiClient {
    /**
     * POSTs to a web service that takes JSON and returns JSON.
     */
    <ResponseType> ResponseType postJsonReturnJson(
            String url,
            B2Headers headersOrNull,
            Object request,
            Class<ResponseType> responseClass) throws B2Exception;

    /**
     * POSTs to a web service that takes content and returns JSON.
     */
    <ResponseType> ResponseType postDataReturnJson(
            String url,
            B2Headers headersOrNull,
            InputStream contentSource,
            long contentLength,
            Class<ResponseType> responseClass) throws B2Exception;

    /**
     * GETs from a web service that returns content.
     */
    public void getContent(String url,
                           B2Headers headersOrNull,
                           B2ContentSink handler) throws B2Exception;
}
