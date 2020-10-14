/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webapihttpclient.android.http_client;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.contentSources.B2HeadersImpl;
import com.backblaze.b2.client.exceptions.*;
import com.backblaze.b2.client.structures.B2ErrorStructure;
import com.backblaze.b2.client.webApiClients.B2WebApiClient;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.json.B2JsonException;
import com.backblaze.b2.json.B2JsonOptions;
import com.backblaze.b2.util.B2Preconditions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import static com.backblaze.b2.util.B2IoUtils.closeQuietly;

public class B2WebApiHttpClientImpl implements B2WebApiClient {
    private final static String UTF8 = "UTF-8";

    private final B2Json bzJson = B2Json.get();
    private final HttpClientFactory clientFactory;

    private B2WebApiHttpClientImpl(HttpClientFactory clientFactory) {
        this.clientFactory = (clientFactory != null) ?
                clientFactory :
                HttpClientFactoryImpl.build();
    }

    @SuppressWarnings("WeakerAccess")
    public static Builder builder() {
        return new Builder();
    }


    @Override
    public <ResponseType> ResponseType postJsonReturnJson(String url,
                                                          B2Headers headersOrNull,
                                                          Object request,
                                                          Class<ResponseType> responseClass) throws B2Exception {

        final String responseString = postJsonAndReturnString(url, headersOrNull, request);

        try {
            return bzJson.fromJson(responseString, responseClass, B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS);
        } catch (B2JsonException e) {
            throw new B2LocalException("parsing_failed", "can't convert response from json: " + e.getMessage(), e);
        }
    }

    @Override
    public <ResponseType> ResponseType postDataReturnJson(String url,
                                                          B2Headers headersOrNull,
                                                          InputStream inputStream,
                                                          long contentLength,
                                                          Class<ResponseType> responseClass) throws B2Exception {
        try {
            InputStream requestEntity = inputStream.setFixedLengthStreamingMode(contentLength);

            String responseJson = postAndReturnString(url, headersOrNull, requestEntity);
            return B2Json.get().fromJson(responseJson, responseClass, B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS);
        } catch (B2JsonException e) {
            throw new B2LocalException("parsing_failed", "can't convert response from json: " + e.getMessage(), e);
        }
    }


    @Override
    public void getContent(String url,
                           B2Headers headersOrNull,
                           B2ContentSink handler) throws B2Exception {

        final URL clientUrl = new URL(url);
        URLConnection client;
        if (clientUrl.getProtocol().equals("https")) {
            client = (HttpsURLConnection) clientUrl.openConnection();
        } else {
            client = (HttpURLConnection) clientUrl.openConnection();
        }
        if (headersOrNull != null) {
            makeHeaders(headersOrNull).forEach((name, value) -> get.setRequestProperty(name, value));
        }
        client.setDoOutput(true);
        try (AutoCloseable cxn = () -> client.disconnect()) {
            int statusCode = client.getResponseCode();
            if (200 <= statusCode && statusCode < 300) {
                InputStream content = client.getInputStream();
                handler.readContent(makeHeaders(client.getHeaderFields()), content);

                // The handler reads the entire contents, but may not make the
                // additional call to read that hits EOF and returns -1.  That
                // last step is necessary for the HTTP library to reuse the
                // connection.  So don't remove this call to read(), even if
                // the logging proves unnecessary.
                //noinspection ResultOfMethodCallIgnored
                content.read();
                //if (content.read() != -1) {
                //    log.warn("handler did not read full response from " + url);
                //}
            } else {
                BufferedReader input = new BufferedReader(new InputStreamReader(client));
                StringBuilder res = new StringBuilder();
                String line;
                while ((line = input.readLine()) != null) {
                    res.append(line);
                }
                input.close();
                throw extractExceptionFromErrorResponse(response, response.toString());
            }
        } catch (IOException e) {
            throw translateToB2Exception(e, url);
        }
    }

    /**
     * HEADSs to a web service that returns content, and returns the headers.
     *
     * @param url the url to head to
     * @param headersOrNull the headers, if any.
     * @return the headers of the response.
     * @throws B2Exception if there's any trouble
     */
    @Override
    public B2Headers head(String url, B2Headers headersOrNull)
            throws B2Exception {
        try {
            URL clientUrl = new URL(url);
            URLConnection head;
            if (clientUrl.getProtocol().equals("https")) {
                 head = (HttpsURLConnection) clientUrl.openConnection();
            } else {
                head = (HttpURLConnection) clientUrl.openConnection();
            }
            if (headersOrNull != null) {
                makeHeaders(headersOrNull).forEach((name, value) -> head.setRequestProperty(name, value));
            }

            head.setDoOutput(true);

            int statusCode = head.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
                B2HeadersImpl.Builder builder = B2HeadersImpl.builder();
                Arrays.stream(head.getHeaderFields().entrySet().forEach(header -> builder.set(header.getName(), header.getValue())));
                return builder.build();
            } else {
                throw B2Exception.create(null, statusCode, null, "");
            }
        } catch (IOException e) {
            throw translateToB2Exception(e, url);
        }
        finally {
            closeQuietly(response);
        }
    }

    @Override
    public void close() {
        clientFactory.close();
    }

    private String postJsonAndReturnString(String url,
                                           B2Headers headersOrNull,
                                           Object request) throws B2Exception {
        String requestString = parseToStringUsingBzJson(request);
        return postAndReturnString(url, headersOrNull, requestString);
    }

    /**
     * POSTs to a web service that returns content, and returns the content
     * as a single string.
     *
     * @param url the url to post to
     * @param headersOrNull the headers, if any.
     * @param requestEntity the entity to post.
     * @return the body of the response.
     * @throws B2Exception if there's any trouble
     */
    private String postAndReturnString(String url, B2Headers headersOrNull, String requestString)
            throws B2Exception {

        try {
            URLConnection post = new URL(url).openConnection();
            if (headersOrNull != null) {
                makeHeaders(headersOrNull).forEach((name, value) -> post.setRequestProperty(name, value));
            }
            if (requestString != null) {
                post.setDoOutput(true);
                try(OutputStream os = post.getOutputStream()) {
                    os.write(requestString, 0, requestString.length);
                } catch (Exception e) {
                    //
                } finally {
                    os.close();
                }
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(post.getInputStream()))) {
                String line;
                StringBuilder responseText = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    responseText.append(line);
                }
            }

            int statusCode = post.getResponseCode();
            if (statusCode >= 200 && statusCode < 300) {
                return responseText;
            } else {
                throw extractExceptionFromErrorResponse(response, responseText);
            }
        } catch (IOException e) {
            throw translateToB2Exception(e, url);
        }
        finally {
            closeQuietly(response);
        }
    }

    private B2Exception translateToB2Exception(IOException e, String url) {
        if (e instanceof ConnectException) {
            // java.net base class for HttpHostConnectException.
            return new B2ConnectFailedException("connect_failed", null, "failed to connect for " + url, e);
        }
        if (e instanceof UnknownHostException) {
            return new B2ConnectFailedException("unknown_host", null, "unknown host for " + url, e);
        }
        if (e instanceof SocketTimeoutException) {
            return new B2NetworkTimeoutException("socket_timeout", null, "socket timed out talking to " + url, e);
        }
        if (e instanceof SocketException) {
            return new B2NetworkException("socket_exception", null, "socket exception talking to " + url, e);
        }
        if (e instanceof MalformedURLException) {
            return new B2NotFoundException("malformed_url", null, "malformed for " + url, e);
        }

        return new B2NetworkException("io_exception", null, e + " talking to " + url, e);
    }

    private B2Exception extractExceptionFromErrorResponse(Class<?> response,
                                                          String responseText) {
        final int statusCode = response.getResponseCode();;
        // Try B2 error structure
        try {
            B2ErrorStructure err = B2Json.get().fromJson(responseText, B2ErrorStructure.class);
            return B2Exception.create(err.code, err.status, getRetryAfterSecondsOrNull(response), err.message);
        }
        catch (Throwable t) {
            // we can't parse the response as a B2 JSON error structure.
            // so use the default.
            return new B2Exception("unknown", statusCode, getRetryAfterSecondsOrNull(response), responseText);
        }
    }

    /**
     * If there's a Retry-After header and it has a delay-seconds formatted value,
     * this returns it.  (to be clear, if there's an HTTP-date value, we ignore it
     * and keep looking for one with delay-seconds format.)
     *
     * @param response the http response.
     * @return the delay-seconds from a Retry-After header, if any.  otherwise, null.
     */
    private Integer getRetryAfterSecondsOrNull(URLConnection response) {
        // https://tools.ietf.org/html/rfc7231#section-7.1.3
        for (String header : response.getHeaderFields().get(B2Headers.RETRY_AFTER)) {
            try {
                return Integer.parseInt(header.getValue(), 10);
            } catch (IllegalArgumentException e) {
                // continue.
            }
        }

        return null;
    }

    private HashMap<String, String> makeHeaders(B2Headers headersOrNull) {
        if (headersOrNull == null) {
            return null;
        }
        final int headerCount = headersOrNull.getNames().size();

        final HashMap<String, String> vHeaders = new HashMap<>();

        int iHeader = 0;
        for (String name : headersOrNull.getNames()) {
            vHeaders.set(name, headersOrNull.getValueOrNull(name));
            iHeader++;
        }

        return vHeaders;
    }


    /**
     * Parse Json using our beloved B2Json
     *
     * @param request the object to be json'ified.
     * @return a new ByteArrayEntity with the json representation of request in it.
     */
    private static String parseToStringUsingBzJson(Object request) throws B2Exception {
        B2Preconditions.checkArgument(request != null);

        try {
            B2Json bzJson = B2Json.get();
            return bzJson.toJson(request);
        } catch (B2JsonException e) {
            //log.warn("Unable to serialize " + request.getClass() + " using B2Json, was passed in request for " + url, ex);
            throw new B2LocalException("parsing_failed", "B2Json.toJson(" + request.getClass() + ") failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the UTF-8 representation of a string.
     */
    private static byte [] getUtf8Bytes(String str) throws B2Exception {
        try {
            return str.getBytes(UTF8);
        } catch (UnsupportedEncodingException e) {
            // this is very, very bad and it's not gonna get better by itself.
            throw new RuntimeException("No UTF-8 charset", e);
        }
    }

    /**
     * This Builder creates HttpClientFactoryImpls.
     * If the httpClientFactory isn't set, a new instance
     * of the default implementation will be used.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Builder {
        private HttpClientFactory httpClientFactory;

        public Builder setHttpClientFactory(HttpClientFactory httpClientFactory) {
            this.httpClientFactory = httpClientFactory;
            return this;
        }

        public B2WebApiHttpClientImpl build() {
            return new B2WebApiHttpClientImpl(httpClientFactory);
        }
    }
}
