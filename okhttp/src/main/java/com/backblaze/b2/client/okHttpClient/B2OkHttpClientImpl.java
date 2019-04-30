/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.okHttpClient;

import com.backblaze.b2.client.contentHandlers.B2ContentSink;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.contentSources.B2HeadersImpl;
import com.backblaze.b2.client.exceptions.*;
import com.backblaze.b2.client.structures.B2ErrorStructure;
import com.backblaze.b2.client.webApiClients.B2WebApiClient;
import com.backblaze.b2.json.B2Json;
import com.backblaze.b2.json.B2JsonException;
import com.backblaze.b2.json.B2JsonOptions;
import okhttp3.*;
import okio.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class B2OkHttpClientImpl implements B2WebApiClient {
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_SOCKET_TIMEOUT_SECONDS = 300;

    private final static String UTF8 = "UTF-8";

    private final B2Json bzJson = B2Json.get();
    private final OkHttpClient okHttpClient;
    private ProgressListener progressListener = null;

    public void setProgressListener(ProgressListener progressListener){
        this.progressListener = progressListener;
    }

    public B2OkHttpClientImpl() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return maybeDecorateResponse(originalResponse);
                })
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_SOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(DEFAULT_SOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_SOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        okHttpClient = builder.build();
    }

    /**
     * Client apps receive download progress notifications through this interface
     */

    public interface ProgressListener {
        /**
         * update client download progress
         * @param b2FileID download file
         * @param bytesRead bytes read so far
         * @param contentLength total size
         * @param done if read exhausted
         */
        void update(String b2FileID, long bytesRead, long contentLength, boolean done);
    }

    /**
     * Decorated response body that reports read progress
     */

    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;
        private final String b2FileID;

        ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener, String b2FileID) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
            this.b2FileID = b2FileID;
        }

        @Override public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override public long contentLength() {
            return responseBody.contentLength();
        }

        @Override public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(b2FileID, totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }

    /**
     * Inspect the original response to see if it is a download. If so, set up progress reporting
     * @param originalResponse response body
     * @return decorated response body or original response body
     */

    private Response maybeDecorateResponse( Response originalResponse ){
        if( progressListener == null){
            return originalResponse;
        } else {
            Request request = originalResponse.request();
            if (request.method().equalsIgnoreCase("GET")
                    && request.url().toString().contains("b2_download_file")) {
                String b2FileID = request.url().queryParameter("fileId");
                return originalResponse.newBuilder()
                        .body(new ProgressResponseBody(originalResponse.body(), progressListener, b2FileID))
                        .build();
            } else {
                return originalResponse;
            }
        }
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
            String responseJson = postAndReturnString(url, headersOrNull, inputStream, contentLength);
            return B2Json.get().fromJson(responseJson, responseClass, B2JsonOptions.DEFAULT_AND_ALLOW_EXTRA_FIELDS);
        } catch (B2JsonException e) {
            throw new B2LocalException("parsing_failed", "can't convert response from json: " + e.getMessage(), e);
        }
    }

    private  B2Headers makeB2Headers( Headers headers){
        if( headers != null ) {
            Map<String, List<String>> headerMap = headers.toMultimap();
            B2HeadersImpl.Builder builder = B2HeadersImpl.builder();

            for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                Iterator it = values.iterator();
                String headerValue = "";
                while( it.hasNext() ){
                    headerValue += it.next();
                    headerValue += it.hasNext() ? "," : "";
                }
                builder.set( key, headerValue);
            }
            return builder.build();
        } else {
            return null;
        }
    }

    private  Headers makeOkHeaders(  B2Headers b2Headers){
        if (b2Headers == null) {
            return null;
        } else {
            Headers.Builder builder = new Headers.Builder();
            for (String name : b2Headers.getNames()) {
                builder.add(name, b2Headers.getValueOrNull(name));
            }
            return builder.build();
        }
    }

    @Override
    public void getContent(String url,
                           B2Headers headersOrNull,
                           B2ContentSink handler) throws B2Exception {
        Request.Builder builder = new Request.Builder()
                .get()
                .url(url);
        Headers headers = makeOkHeaders(headersOrNull);
        if( headers != null ){
            builder.headers(headers);
        }
        Request request = builder.build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw extractExceptionFromErrorResponse(response, response.body().string());
            } else {
                if( response.body() != null ){
                    Headers responseHeaders = response.headers();
                    B2Headers b2headers = makeB2Headers(responseHeaders);
                    handler.readContent(b2headers, response.body().byteStream());
                }
            }
        } catch (IOException e) {
            translateToB2Exception(e, url);
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
        Request.Builder builder = new Request.Builder()
                .url(url)
                .head();
        Headers reqHeaders = makeOkHeaders(headersOrNull);
        if( reqHeaders != null ){
            builder.headers(reqHeaders);
        }
        Request request = builder.build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw extractExceptionFromErrorResponse(response, response.body().string());
            } else {
                if( response.code() == 200){
                    Headers responseHeaders = response.headers();
                    B2Headers b2headers = makeB2Headers(responseHeaders);
                    return b2headers;
                } else {
                    throw B2Exception.create(null, response.code(), null, "");
                }
            }
        } catch (IOException e) {
            translateToB2Exception(e, url);
        }
        return null;
    }

    @Override
    public void close() {
        // todo: Closing response body streams is handled elsewhere by OkHttp. Do we need anything here?
        // clientFactory.close();
    }

    private String postJsonAndReturnString(String url,
                                           B2Headers headersOrNull,
                                           Object request) throws B2Exception {
        try {
            B2Json bzJson = B2Json.get();
            String requestJson = bzJson.toJson(request);
            byte[] requestBytes = getUtf8Bytes(requestJson);
            return postAndReturnString(url, headersOrNull,new ByteArrayInputStream(requestBytes), requestJson.length());
        } catch (B2JsonException e) {
            //log.warn("Unable to serialize " + request.getClass() + " using B2Json, was passed in request for " + url, ex);
            throw new B2LocalException("parsing_failed", "B2Json.toJson(" + request.getClass() + ") failed: " + e.getMessage(), e);
        }

    }

    private byte[] readFully(InputStream inputStream, int contentLength) throws IOException{
        int numRead = 0;
        int offset = 0;
        int length = contentLength;
        byte[] bytes = new byte[contentLength];
        while( numRead < contentLength ){
            int n = inputStream.read(bytes, offset, length);
            if( n > -1 ){
                numRead += n;
                offset += n;
                length = contentLength - numRead;
            }
        }
        return bytes;
    }

    /**
     * POSTs to a web service that returns content, and returns the content
     * as a single string.
     *
     * @param url the url to post to
     * @param headersOrNull the headers, if any.
     *
     * @return the body of the response.
     * @throws B2Exception if there's any trouble
     */
    private  String postAndReturnString( String url,  B2Headers headersOrNull,  InputStream inputStream, long contentLength)
            throws B2Exception {
        byte[] bytes;
        try {
            bytes = readFully(inputStream, (int)contentLength);
        } catch (IOException e) {
            throw translateToB2Exception(e, url);
        }
        RequestBody body = RequestBody.create(MediaType.get("application/json"), bytes);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);
        if( headersOrNull != null ){
            builder.headers(makeOkHeaders(headersOrNull));
        }
        Request request = builder.build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw extractExceptionFromErrorResponse(response, response.body().string());
            } else {
                if( response.code() == 200){
                    return response.body().string();
                } else {
                    throw extractExceptionFromErrorResponse(response, response.body().toString());
                }
            }
        } catch (IOException e) {
            translateToB2Exception(e, url);
        }
        return null;
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

        return new B2NetworkException("io_exception", null, e + " talking to " + url, e);
    }

    private B2Exception extractExceptionFromErrorResponse(Response response,
                                                          String responseText) {
        final int statusCode = response.code();

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
    private Integer getRetryAfterSecondsOrNull(Response response) {
        // https://tools.ietf.org/html/rfc7231#section-7.1.3
        List<String> retryAfterHeaderVals = response.headers(B2Headers.RETRY_AFTER);
        for (String retryAfter : retryAfterHeaderVals) {
            try {
                return Integer.parseInt(retryAfter, 10);
            } catch (IllegalArgumentException e) {
                // continue.
            }
        }

        return null;
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
}
