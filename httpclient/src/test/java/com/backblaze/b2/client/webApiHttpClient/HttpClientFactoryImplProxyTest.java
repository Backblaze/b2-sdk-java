/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the default HttpClientFactory honors the JVM's proxy settings,
 * using a local "proxy" that answers every CONNECT with a 502.
 */
public class HttpClientFactoryImplProxyTest {
    private static final String[] PROXY_PROPERTIES = {
            "https.proxyHost", "https.proxyPort", "http.nonProxyHosts"
    };

    private final String[] savedProperties = new String[PROXY_PROPERTIES.length];
    private final BlockingQueue<String> requestLines = new LinkedBlockingQueue<>();
    private ServerSocket proxy;
    private Thread proxyThread;

    @Before
    public void setUp() throws IOException {
        for (int i = 0; i < PROXY_PROPERTIES.length; i++) {
            savedProperties[i] = System.getProperty(PROXY_PROPERTIES[i]);
        }
        // the JVM's default for this skips proxies for localhost.
        System.setProperty("http.nonProxyHosts", "");

        proxy = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
        proxyThread = new Thread(this::serve);
        proxyThread.setDaemon(true);
        proxyThread.start();

        System.setProperty("https.proxyHost", "localhost");
        System.setProperty("https.proxyPort", String.valueOf(proxy.getLocalPort()));
    }

    @After
    public void tearDown() throws IOException {
        for (int i = 0; i < PROXY_PROPERTIES.length; i++) {
            if (savedProperties[i] == null) {
                System.clearProperty(PROXY_PROPERTIES[i]);
            } else {
                System.setProperty(PROXY_PROPERTIES[i], savedProperties[i]);
            }
        }
        proxy.close();
    }

    private void serve() {
        while (!proxy.isClosed()) {
            try (Socket socket = proxy.accept()) {
                final BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                requestLines.add(in.readLine());
                final OutputStream out = socket.getOutputStream();
                out.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                        .getBytes(StandardCharsets.US_ASCII));
                out.flush();
            } catch (IOException e) {
                // closed in tearDown()
            }
        }
    }

    private static void tryGet(HttpClientFactoryImpl.Builder builder, String url) throws Exception {
        try (HttpClientFactoryImpl factory = builder.build();
             CloseableHttpClient client = factory.create()) {
            client.execute(new HttpGet(url)).close();
        } catch (IOException e) {
            // expected for https: the fake proxy refuses the tunnel.
        }
    }

    @Test
    public void testHttpsRequestGoesThroughSystemProxy() throws Exception {
        tryGet(HttpClientFactoryImpl.builder(), "https://b2.invalid/");

        final String line = requestLines.poll(5, TimeUnit.SECONDS);
        assertTrue("proxy should have received a CONNECT, got: " + line,
                line != null && line.startsWith("CONNECT b2.invalid:443 "));
    }

    @Test
    public void testSystemProxyCanBeDisabled() throws Exception {
        tryGet(HttpClientFactoryImpl.builder().setUseSystemProxy(false), "https://b2.invalid/");

        assertNull(requestLines.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testHttpDestinationStillRejectedByDefault() throws Exception {
        tryGet(HttpClientFactoryImpl.builder(), "http://b2.invalid/");

        assertNull(requestLines.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testHttpDestinationAllowedWhenInsecureHttpEnabled() throws Exception {
        // http destinations use the (default) http proxy settings, not https.proxyHost,
        // so this only checks that the request is not rejected up front.
        System.setProperty("http.proxyHost", "localhost");
        try {
            System.setProperty("http.proxyPort", String.valueOf(proxy.getLocalPort()));
            tryGet(HttpClientFactoryImpl.builder().setSupportInsecureHttp(true), "http://b2.invalid/");
            assertTrue(requestLines.poll(5, TimeUnit.SECONDS).startsWith("GET http://b2.invalid/ "));
        } finally {
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
        }
    }
}
