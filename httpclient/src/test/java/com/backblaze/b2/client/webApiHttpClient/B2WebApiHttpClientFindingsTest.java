/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import com.backblaze.b2.testing.KnownDefect;
import org.junit.experimental.categories.Category;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.contentSources.B2HeadersImpl;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2NetworkTimeoutException;
import com.backblaze.b2.client.webApiClients.B2WebApiClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class B2WebApiHttpClientFindingsTest {
    private HttpServer server;
    private String baseUrl;
    private volatile Handler handler;

    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }

    @Before
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 10);
        server.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/x";
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    private static B2WebApiClient clientWithSocketTimeoutSeconds(int secs) {
        return B2WebApiHttpClientImpl.builder()
                .setHttpClientFactory(HttpClientFactoryImpl.builder()
                        .setSupportInsecureHttp(true)
                        .setSocketTimeoutSeconds(secs)
                        .build())
                .build();
    }

    private static B2WebApiClient defaultClient() {
        return clientWithSocketTimeoutSeconds(20);
    }

    private static void reply(HttpExchange ex, int status, String body, String... headerNameValuePairs) throws IOException {
        for (int i = 0; i < headerNameValuePairs.length; i += 2) {
            ex.getResponseHeaders().add(headerNameValuePairs[i], headerNameValuePairs[i + 1]);
        }
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if ("HEAD".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(status, -1);
        } else {
            ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                ex.getResponseBody().write(bytes);
            }
        }
    }

    /** Finding C25: downloads must honour the read timeout configured on the HTTP factory. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC25_downloadHonoursConfiguredSocketTimeout() throws Exception {
        handler = ex -> {
            try {
                Thread.sleep(3500); // the server stalls before answering
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            reply(ex, 200, "late");
        };
        final B2WebApiClient client = clientWithSocketTimeoutSeconds(1);
        final long start = System.nanoTime();
        try {
            client.getContent(baseUrl, null, B2ContentMemoryWriter.build());
            fail("getContent waited " + (System.nanoTime() - start) / 1_000_000 + " ms although the socket timeout was 1 s");
        } catch (B2NetworkTimeoutException expected) {
            // expected
        } finally {
            client.close();
        }
    }

    /** Finding C31: a failed HEAD must report the server's Retry-After and a usable status. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC31_headFailureKeepsRetryAfter() throws Exception {
        handler = ex -> reply(ex, 503, "", "Retry-After", "7");
        final B2WebApiClient client = defaultClient();
        try {
            client.head(baseUrl, null);
            fail("expected an exception");
        } catch (B2Exception e) {
            assertEquals(503, e.getStatus());
            assertEquals("Retry-After was dropped", Integer.valueOf(7), e.getRetryAfterSecondsOrNull());
            assertNotNull("exception has no error code", e.getCode());
        } finally {
            client.close();
        }
    }

    /** Finding C32: an error reply with an extra, unknown JSON field must still yield the real error code. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC32_errorBodyWithExtraFieldKeepsCode() throws Exception {
        handler = ex -> reply(ex, 500, "{\"status\":500,\"code\":\"bad\",\"message\":\"went wrong\",\"newField\":\"x\"}",
                "Content-Type", "application/json");
        final B2WebApiClient client = defaultClient();
        try {
            client.getContent(baseUrl, null, B2ContentMemoryWriter.build());
            fail("expected an exception");
        } catch (B2Exception e) {
            assertEquals("bad", e.getCode());
            assertEquals("went wrong", e.getMessage());
        } finally {
            client.close();
        }
    }

    /** Finding C11 (parse side): a negative Retry-After from the server must not be passed on as a wait time. */
    @Category(KnownDefect.class)
    @Test
    public void testFindingC11_negativeRetryAfterHeaderIsNotTrusted() throws Exception {
        handler = ex -> reply(ex, 503, "{\"status\":503,\"code\":\"service_unavailable\",\"message\":\"m\"}",
                "Retry-After", "-5");
        final B2WebApiClient client = defaultClient();
        try {
            client.getContent(baseUrl, null, B2ContentMemoryWriter.build());
            fail("expected an exception");
        } catch (B2Exception e) {
            final Integer retryAfter = e.getRetryAfterSecondsOrNull();
            assertTrue("negative wait passed on: " + retryAfter, retryAfter == null || retryAfter >= 0);
        } finally {
            client.close();
        }
    }

    /**
     * Finding C46 (stronger replacement for the two download-error tests that lack fail()):
     * a 500 reply must make getContent throw; passing silently is a failure.
     */
    @Test
    public void testFindingC46_errorResponseMustThrow() throws Exception {
        handler = ex -> reply(ex, 500, "something went wrong", "Content-Type", "text/plain");
        final B2WebApiClient client = defaultClient();
        try {
            client.getContent(baseUrl, null, B2ContentMemoryWriter.build());
            fail("getContent did not throw on a 500 reply");
        } catch (B2Exception e) {
            assertEquals(500, e.getStatus());
            assertEquals("unknown", e.getCode());
            assertEquals("something went wrong", e.getMessage());
        } finally {
            client.close();
        }
    }

    /**
     * Finding C47: the existing "compressed" test sets no Content-Encoding header, so it cannot notice if
     * the client starts decompressing.  Here the reply is marked Content-Encoding: gzip and the client
     * must hand over the stored (compressed) bytes untouched.
     */
    @Test
    public void testFindingC47_contentEncodedDownloadIsNotDecompressed() throws Exception {
        final byte[] gzipped = gzip("hello hello hello hello hello".getBytes(StandardCharsets.UTF_8));
        handler = ex -> {
            ex.getResponseHeaders().add("Content-Encoding", "gzip");
            ex.sendResponseHeaders(200, gzipped.length);
            ex.getResponseBody().write(gzipped);
        };
        final B2WebApiClient client = defaultClient();
        try {
            final B2ContentMemoryWriter sink = B2ContentMemoryWriter.build();
            client.getContent(baseUrl, B2HeadersImpl.builder().build(), sink);
            org.junit.Assert.assertArrayEquals(gzipped, sink.getBytes());
        } finally {
            client.close();
        }
    }

    private static byte[] gzip(byte[] in) throws IOException {
        final java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(bos)) {
            gz.write(in);
        }
        return bos.toByteArray();
    }
}
