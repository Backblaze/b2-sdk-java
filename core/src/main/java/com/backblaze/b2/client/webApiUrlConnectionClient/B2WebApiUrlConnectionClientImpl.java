/*
 * Copyright 2019, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiUrlConnectionClient;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.contentSources.B2HeadersImpl;
import com.backblaze.b2.client.exceptions.B2ConnectFailedException;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.exceptions.B2NetworkException;
import com.backblaze.b2.client.exceptions.B2NetworkTimeoutException;
import com.backblaze.b2.client.structures.B2ErrorStructure;
import com.backblaze.b2.client.webApiClients.B2WebApiClient;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.json.B2JsonException;
import com.backblaze.b2.json.B2JsonOptions;
import com.backblaze.b2.util.B2IoUtils;
import com.backblaze.b2.util.B2StringUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static com.backblaze.b2.util.B2DateTimeUtil.ONE_SECOND_IN_MILLIS;

// XXX: make sure all the code is getting used.
// XXX: connect and read (and post?) socket timeouts?

public class B2WebApiUrlConnectionClientImpl implements B2WebApiClient {
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_SOCKET_TIMEOUT_SECONDS = 20;

    private final B2Json bzJson = B2Json.get();

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
    public <ResponseType> ResponseType postJsonReturnJson(
            String url,
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

    /**
     * POSTs to a web service that takes content and returns JSON.
     *
     * @param url the url to post to
     * @param headersOrNull if non-null, some headers to include in the POST
     * @param contentStream the content to post as the body of the request
     * @param contentLength the number of bytes in the content to post
     * @param responseClass the class to convert the response body into (with B2Json)
     * @param <ResponseType> the class
     * @return the response object (converted from json by B2Json)
     * @throws B2Exception if there's any trouble
     */
    public <ResponseType> ResponseType postDataReturnJson(
            String url,
            B2Headers headersOrNull,
            InputStream contentStream,
            long contentLength,
            Class<ResponseType> responseClass) throws B2Exception {
        try {
            String responseJson = postAndReturnString(url, headersOrNull, contentLength, contentStream);
            return B2Json.get().fromJson(responseJson, responseClass, B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS);
        } catch (B2JsonException e) {
            throw new B2LocalException("parsing_failed", "can't convert response from json: " + e.getMessage(), e);
        }
    }

    /**
     * GETs from a web service that returns content.
     *
     * @param urlString the url to post to
     * @param headersOrNull if non-null, some headers to include in the GET
     * @param handler the object which will be called with the response's headers and content
     * @throws B2Exception if there's any trouble
     */
    public void getContent(String urlString,
                           B2Headers headersOrNull,
                           B2ContentSink handler) throws B2Exception {
        try {
            final HttpURLConnection httpCxn = openHttpConnection(urlString);
            configureHttpCxn(httpCxn, 0L, headersOrNull);

            final int statusCode = httpCxn.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK || statusCode == HttpURLConnection.HTTP_PARTIAL) {
                handler.readContent(makeB2Headers(httpCxn), httpCxn.getInputStream());
            } else {
                throw extractExceptionFromErrorResponse(statusCode, httpCxn);
            }
        } catch (IOException e) {
            throw translateToB2Exception(e, urlString);
        }
    }

    /**
     * HEADSs to a web service that returns content, and returns the headers.
     *
     * @param urlString the url to head to
     * @param headersOrNull the headers, if any.
     * @return the headers of the response.
     * @throws B2Exception if there's any trouble
     */
    public B2Headers head(String urlString, B2Headers headersOrNull) throws B2Exception {
        try {
            final HttpURLConnection httpCxn = openHttpConnection(urlString);

            // set method & say there will be input
            httpCxn.setRequestMethod("HEAD");
            httpCxn.setDoInput(true);

            configureHttpCxn(httpCxn, 0L, headersOrNull);

            final int statusCode = httpCxn.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                return makeB2Headers(httpCxn);
            } else {
                throw extractExceptionFromErrorResponse(statusCode, httpCxn);
            }
        } catch (IOException e) {
            throw translateToB2Exception(e, urlString);
        }
    }

    /**
     * Adds headers to the given http connection, including some we always add
     * and including the ones in headersOrNull if it's not null.
     * @param httpCxn the cxn to add the headers to
     * @param contentLength the Content-Length
     * @param headersOrNull if non-null, the headers to add to the connection.
     */
    private void configureHttpCxn(HttpURLConnection httpCxn, long contentLength, B2Headers headersOrNull) {
        // android tries to get the server to gzip responses.  that makes the
        // Content-Length not contain the size of the object being requested
        // from B2.  until we figure out whether that's safe, let's disable
        // that behavior.  see:
        //     https://android-developers.googleblog.com/2011/09/androids-http-clients.html
        //     https://developer.android.com/about/versions/marshmallow/android-6.0-changes
        httpCxn.setRequestProperty("Accept-Encoding", "identity");

        if (contentLength > 0) {
            // this prevents us from buffering the content before posting it.
            // it also keeps the httpCxn from being able to handle redirects automatically,
            // but we're ok with that.  AND, i think it sets the Content-Length header.
            httpCxn.setFixedLengthStreamingMode(contentLength);
        }

        // disable caches (XXX: revisit?)
        httpCxn.setUseCaches(false);

        // set some timeouts
        httpCxn.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_SECONDS * (int) ONE_SECOND_IN_MILLIS);
        httpCxn.setConnectTimeout(DEFAULT_SOCKET_TIMEOUT_SECONDS * (int) ONE_SECOND_IN_MILLIS);


        // set headers provided by caller.
        if (headersOrNull != null) {
            for (String name : headersOrNull.getNames()) {
                httpCxn.setRequestProperty(name, headersOrNull.getValueOrNull(name));
            }
        }
    }

    private B2Headers makeB2Headers(HttpURLConnection httpCxn) {
        final B2HeadersImpl.Builder builder = B2HeadersImpl.builder();
        for (Map.Entry<String,List<String>> entry : httpCxn.getHeaderFields().entrySet()) {
            // there's an entry with the key null which contains the HTTP response status line!
            final String name = entry.getKey();
            if (name != null) {
                final StringJoiner joiner = new StringJoiner(",");
                entry.getValue().forEach(joiner::add);
                builder.set(name, joiner.toString());
            }
        }
        return builder.build();
    }

    /**
     * Closes this object and its underlying resources.
     * This is overridden from AutoCloseable to declare that it can't throw any exception.
     */
    @Override
    public void close() {
    }


    private String postJsonAndReturnString(String url,
                                           B2Headers headersOrNull,
                                           Object request) throws B2Exception {
        try {
            final byte[] bytesToPost = bzJson.toJsonUtf8Bytes(request);
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(bytesToPost);
            return postAndReturnString(url, headersOrNull, bytesToPost.length, inputStream);
        } catch (B2JsonException e) {
            throw new B2LocalException("bad_argument", "failed to convert object to json: " + e, e);
        }
    }

    /**
     * POSTs to a web service that returns content, and returns the content
     * as a single string.
     *
     * @param urlString the url to post to
     * @param headersOrNull the headers, if any.
     * @param contentLength the Content-Length
     * @param inputStream the data to post.
     * @return the body of the response.
     * @throws B2Exception if there's any trouble
     */
    private String postAndReturnString(String urlString, B2Headers headersOrNull, long contentLength, InputStream inputStream)
            throws B2Exception {

        try {
            final HttpURLConnection httpCxn = openHttpConnection(urlString);

            // set method & say there will be input
            httpCxn.setRequestMethod("POST");
            httpCxn.setDoInput(true);
            httpCxn.setDoOutput(true);

            configureHttpCxn(httpCxn, contentLength, headersOrNull);

            try (OutputStream outputStream = httpCxn.getOutputStream()) {
                B2IoUtils.copy(inputStream, outputStream);
                outputStream.flush();
            }

            final int statusCode = httpCxn.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                return streamToString(httpCxn.getInputStream());
            } else {
                throw extractExceptionFromErrorResponse(statusCode, httpCxn);
            }
        } catch (IOException e) {
            throw translateToB2Exception(e, urlString);
        }
    }

    private String streamToString(InputStream inputStream) throws IOException {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        B2IoUtils.copy(inputStream, byteStream);
        return byteStream.toString(B2StringUtil.UTF8);
    }

    private HttpURLConnection openHttpConnection(String urlString) throws IOException, B2LocalException {
        HttpURLConnection httpCxn;
        final URL url = new URL(urlString);
        final URLConnection urlCxn = url.openConnection();

        if (!(urlCxn instanceof HttpURLConnection)) {
            throw new B2LocalException("bad_url", "url isn't http-ish? (" + url + ")", null);
        }
        httpCxn = (HttpURLConnection) urlCxn;
        return httpCxn;
    }

    private B2Exception extractExceptionFromErrorResponse(int statusCode, HttpURLConnection urlCxn) {
        String responseText = null;

        // Try B2 error structure
        try {
            responseText = streamToString(urlCxn.getErrorStream());
            B2ErrorStructure err = B2Json.get().fromJson(responseText, B2ErrorStructure.class);
            return B2Exception.create(err.code, err.status, getRetryAfterSecondsOrNull(urlCxn), err.message);
        }
        catch (Throwable t) {
            // we can't parse the response as a B2 JSON error structure.
            // so use the default.
            final String errMsg = (responseText != null) ?
                    responseText :
                    "" + t.getMessage(); // make sure this isn't null.
            return new B2Exception("unknown", statusCode, getRetryAfterSecondsOrNull(urlCxn), errMsg);
        }
    }


    private B2Exception translateToB2Exception(IOException e, String url) {
        if (e instanceof MalformedURLException) {
            // from HTTP Components
            return new B2LocalException("local", "bad url: " + url, e);
        }
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

        return new B2NetworkException("io_exception", null, e + " talking to " + url, e);
    }
    /**
     * If there's a Retry-After header and it has a delay-seconds formatted value,
     * this returns it.  (to be clear, if there's an HTTP-date value, we ignore it
     * and keep looking for one with delay-seconds format.)
     *
     * @param httpCxn the http connection which should have a response.
     * @return the delay-seconds from a Retry-After header, if any.  otherwise, null.
     */
    private Integer getRetryAfterSecondsOrNull(HttpURLConnection httpCxn) {
        // https://tools.ietf.org/html/rfc7231#section-7.1.3
        final List<String> valuesOrNull = httpCxn.getHeaderFields().get(B2Headers.RETRY_AFTER);
        if (valuesOrNull != null) {
            for (String value : valuesOrNull) {
                try {
                    return Integer.parseInt(value, 10);
                } catch (IllegalArgumentException e) {
                    // continue.
                }
            }
        }
        return null;
    }
}
