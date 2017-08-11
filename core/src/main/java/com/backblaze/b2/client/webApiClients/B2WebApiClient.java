/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiClients;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;

import java.io.InputStream;

public interface B2WebApiClient extends AutoCloseable {
    /**
     * POSTs to a web service that takes JSON and returns JSON.
     *
     * @param url the url to post to
     * @param headersOrNull if non-null, some headers to include in the post
     * @param request the object to be converted to json (with B2Json) and posted as the body of the request
     * @param responseClass the class to convert the response body into (with B2Json)
     * @param <ResponseType> the class
     * @return the response object (converted from json by B2Json)
     * @throws B2Exception if there's any trouble
     */
    <ResponseType> ResponseType postJsonReturnJson(
            String url,
            B2Headers headersOrNull,
            Object request,
            Class<ResponseType> responseClass) throws B2Exception;

    /**
     * POSTs to a web service that takes content and returns JSON.
     *
     * @param url the url to post to
     * @param headersOrNull if non-null, some headers to include in the post
     * @param contentSource the content to post as the body of the request
     * @param contentLength the number of bytes in the content to post
     * @param responseClass the class to convert the response body into (with B2Json)
     * @param <ResponseType> the class
     * @return the response object (converted from json by B2Json)
     * @throws B2Exception if there's any trouble
     */
    <ResponseType> ResponseType postDataReturnJson(
            String url,
            B2Headers headersOrNull,
            InputStream contentSource,
            long contentLength,
            Class<ResponseType> responseClass) throws B2Exception;

    /**
     * GETs from a web service that returns content.
     *
     * @param url the url to post to
     * @param headersOrNull if non-null, some headers to include in the post
     * @param handler the object which will be called with the response's headers and content
     * @throws B2Exception if there's any trouble
     */
    void getContent(String url,
                    B2Headers headersOrNull,
                    B2ContentSink handler) throws B2Exception;

    /**
     * Closes this object and its underlying resources.
     * This is overridden from AutoCloseable to declare that it can't throw any exception.
     */
    @Override
    void close();
}
