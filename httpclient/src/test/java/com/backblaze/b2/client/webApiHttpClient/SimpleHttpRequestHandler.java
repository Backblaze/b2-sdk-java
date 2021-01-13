/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple Http request handler hosted by SimpleHttpServer
 */
public class SimpleHttpRequestHandler {
    private Response nextResponse;

    /* method, headers, and request body to be saved */
    private String lastRequestMethod;
    private Map<String, String> lastRequestHeaders;
    private byte[] lastRequestBody;

    /**
     * set the next response to return
     * @param response the Response object to be saved
     */
    synchronized public void setNextResponse(Response response) {
        nextResponse = response;
    }

    /**
     * Handle the request by returning the preset response.
     *
     * @param requestMethod request method.
     * @param headers HTTP headers from the request.
     * @param requestBody for POST, body of the request.
     *                    for GET, the query part of the url or "".
     *                    otherwise null.
     * @return the http response.
     */
    synchronized public Response handleRequest(String requestMethod, Map<String, String> headers, byte[] requestBody) {
        lastRequestMethod = requestMethod;
        lastRequestHeaders = headers;
        lastRequestBody = requestBody;

        return nextResponse;
    }

    /**
     * return the saved last request method
     * @return the last request method
     */
    synchronized public String getLastRequestMethod() {
        return this.lastRequestMethod;
    }

    /**
     * return the saved last request headers
     * @return the request headers in a hash map
     */
    synchronized public Map<String, String> getLastRequestHeaders() {
        return this.lastRequestHeaders;
    }

    /**
     * return the saved last request body
     * @return the request body in byte array
     */
    synchronized public byte[] getLastRequestBody() {
        return this.lastRequestBody;
    }

    /**
     * Http response to be returned by the handler
     */
    public static class Response {
        public final int statusCode;
        public final String contentType;
        public final byte[] data;
        public final Map<String, String> headers;

        public Response(int statusCode, String contentType, byte[] data, Map<String, String> headers) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.data = data;
            this.headers = headers;
        }
    }

    /**
     * create a http response instance
     *
     * @param statusCode the http status code
     * @param contentType the content type of response body
     * @param data response body data
     * @param headers response headers
     * @return a http response instance based on the data provided
     */
    public static Response createResponse(int statusCode, String contentType, byte[] data, Map<String, String> headers) {
        final Map<String, String> headerMap = new HashMap<>();
        if (headers != null) {
            headerMap.putAll(headers);
        }
        return new Response(statusCode, contentType, data, headerMap);
    }

    public static Response createResponse(int statusCode, String contentType, byte[] data) {
        return createResponse(statusCode, contentType, data, null);
    }
}
