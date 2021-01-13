/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.contentSources.B2HeadersImpl;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2ErrorStructure;
import com.backblaze.b2.client.webApiClients.B2WebApiClient;
import com.backblaze.b2.json.B2Json;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.Deflater;

import static com.backblaze.b2.client.webApiHttpClient.SimpleHttpRequestHandler.createResponse;
import static org.junit.Assert.*;

/**
 * Unit test for B2WebApiHttpClientImpl
 *
 * utilize a local web server to handle requests and generate responses
 */
public class B2WebApiHttpClientImplTest {

    /* simple embedded http server */
    private SimpleHttpServer server;

    /* URL to make a call to the web server */
    private String url;

    /* The response handler for the web server, we will set its response to return */
    private static final SimpleHttpRequestHandler requestHandler = new SimpleHttpRequestHandler();

    // B2WebApiClient to be tested
    private static final B2WebApiClient b2WebApiClient = B2WebApiHttpClientImpl.builder()
            .setHttpClientFactory(HttpClientFactoryImpl.builder()
                    .setSupportInsecureHttp(true) // insecure http for testing
                    .build())
            .build();

    private static final Map<String, String> REQUEST_HEADERS = new LinkedHashMap<String, String>() {{
        put("YI", "Y");
        put("ER", "E");
        put("SAN", "S");
    }};

    private static final Map<String, String> RESPONSE_HEADERS = new LinkedHashMap<String, String>() {{
        put("one", "1");
        put("two", "2");
        put("three", "3");
    }};

    private static final SimpleHttpRequestHandler.Response LARGE_CONTENT_RESPONSE = createResponse(
            HttpStatus.SC_OK,
            ContentType.TEXT_PLAIN.toString(),
            makeLargeTextString(),
            RESPONSE_HEADERS
    );

    private static final SimpleHttpRequestHandler.Response COMPRESSED_LARGE_CONTENT_RESPONSE = createResponse(
            HttpStatus.SC_OK,
            ContentType.TEXT_PLAIN.toString(),
            makeCompressedLargeTextString(makeLargeTextString()),
            RESPONSE_HEADERS
    );

    private static final B2ErrorStructure INTERNAL_ERROR_STRUCTURE = new B2ErrorStructure(
            HttpStatus.SC_INTERNAL_SERVER_ERROR,
            "bad",
            "something went wrong");


    private static final SimpleHttpRequestHandler.Response JSON_ERROR_RESPONSE = createResponse(
            HttpStatus.SC_INTERNAL_SERVER_ERROR,
            ContentType.APPLICATION_JSON.toString(),
            B2Json.toJsonOrThrowRuntime(INTERNAL_ERROR_STRUCTURE).getBytes()
    );

    @Test
    public void testGetContent() throws B2Exception {
        // large content with enableContentCompression set to true
        doTestGetContent(REQUEST_HEADERS, LARGE_CONTENT_RESPONSE, RESPONSE_HEADERS);

        // compressed large content with false enableContentCompression
        doTestGetContent(REQUEST_HEADERS, COMPRESSED_LARGE_CONTENT_RESPONSE, RESPONSE_HEADERS);

        // error case
        doTestGetContentWithException(REQUEST_HEADERS, JSON_ERROR_RESPONSE, RESPONSE_HEADERS);
    }

    private void doTestGetContentWithException(Map<String, String> requestHeaders, SimpleHttpRequestHandler.Response expectedResponse, Map<String, String> responseHeaders) {
        final B2HeadersImpl.Builder requestHeaderBuilder = B2HeadersImpl.builder();
        requestHeaders.forEach(requestHeaderBuilder::set);
        requestHandler.setNextResponse(expectedResponse);

        final B2ContentMemoryWriter sink = B2ContentMemoryWriter.build();

        // call getContent and verify the returned exception response data
        try {
            b2WebApiClient.getContent(url, requestHeaderBuilder.build(), sink);
        } catch (B2Exception b2Exception) {
            assertEquals(INTERNAL_ERROR_STRUCTURE.status, b2Exception.getStatus());
            assertEquals(INTERNAL_ERROR_STRUCTURE.code, b2Exception.getCode());
            assertEquals(INTERNAL_ERROR_STRUCTURE.message, b2Exception.getMessage());
        }
    }

    private void doTestGetContent(Map<String, String> requestHeaders, SimpleHttpRequestHandler.Response expectedResponse, Map<String, String> responseHeaders) throws B2Exception {
        final B2HeadersImpl.Builder requestHeaderBuilder = B2HeadersImpl.builder();
        requestHeaders.forEach(requestHeaderBuilder::set);
        requestHandler.setNextResponse(expectedResponse);

        final B2ContentMemoryWriter sink = B2ContentMemoryWriter.build();

        // call getContent
        b2WebApiClient.getContent(url,  requestHeaderBuilder.build(), sink);

        // verify request data
        checkGetContentRequestData(requestHeaderBuilder.build(), new byte[0], requestHandler.getLastRequestMethod(), requestHandler.getLastRequestHeaders(), requestHandler.getLastRequestBody());

        // check if response data matches the expected
        checkGetContentResponseData(makeResponseHeaders(responseHeaders), expectedResponse.data, sink.getHeadersOrNull(), sink.getBytes());
    }

    private B2Headers makeResponseHeaders(Map<String, String> responseHeaders) {
        final B2HeadersImpl.Builder responseHeaderBuilder = B2HeadersImpl.builder();
        responseHeaders.forEach(responseHeaderBuilder::set);
        return responseHeaderBuilder.build();
    }

    /* check request headers and body content bytes */
    private void checkGetContentRequestData(B2Headers expectedHeaders, byte[] expectedContent, String httpMethod, Map<String, String> actualHeaders, byte[] actualContent) {
        final B2HeadersImpl.Builder builder = B2HeadersImpl.builder();
        actualHeaders.forEach(builder::set);
        checkExpectedB2HeadersAndContentMatchActual(expectedHeaders, expectedContent, builder.build(), actualContent);
        assertEquals("GET", httpMethod);
    }

    /* check response headers and content bytes */
    private void checkGetContentResponseData(B2Headers expectedHeaders, byte[] expectedContent, B2Headers actualHeaders, byte[] actualContent) {
        checkExpectedB2HeadersAndContentMatchActual(expectedHeaders, expectedContent, actualHeaders, actualContent);
    }

    private void checkExpectedB2HeadersAndContentMatchActual(B2Headers expectedHeaders, byte[] expectedContent, B2Headers actualHeaders, byte[] actualContent) {
        assertArrayEquals(expectedContent, actualContent);
        assertTrue(expectedHeaders.getNames().stream().allMatch(name -> actualHeaders.getNames().contains(name) &&
                actualHeaders.getValueOrNull(name).equals(expectedHeaders.getValueOrNull(name))));
    }

    @Before
    public final void setupLocal() throws Exception {
        server = new SimpleHttpServer(0, 10);
        server.addPath("/test", requestHandler);
        url = "http://127.0.0.1:" + server.getPort() + "/test";
        Thread.sleep(100); // pause a bit before actual tests
    }

    @After
    public final void cleanupLocal() {
        server.close();
    }

    /**
     * Create a 1,000,000 byte array of the alphabet repeated
     */
    private static byte[] makeLargeTextString() {
        final byte[] retval = new byte[1000000];

        for (int i = 0; i < 1000000; ++i) {
            retval[i] = (byte) ((i % 26) + 'a');
        }
        return retval;
    }

    /**
     * generate the compressed version of large text string in byte array
     * @param input uncompressed byte array
     * @return compressed byte array
     */
    private static byte[] makeCompressedLargeTextString(byte[] input) {
        final Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();
        final byte[] buffer = new byte[2000000]; // big enough for the compressed content
        final int compressedLength = deflater.deflate(buffer);
        final String compressedContent = new String(buffer, 0, compressedLength);
        return compressedContent.getBytes();
    }
}
