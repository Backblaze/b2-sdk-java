/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import org.junit.Test;

import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class B2WebApiHttpClientImplTimeoutTest {
    @Test
    public void testGetContentHonorsConfiguredSocketTimeout() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            Thread t = new Thread(() -> {
                try (Socket s = ss.accept()) {
                    s.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\n\r\nabc".getBytes());
                    s.getOutputStream().flush();
                    Thread.sleep(30000);
                } catch (Exception ignore) { }
            });
            t.setDaemon(true);
            t.start();
            HttpClientFactoryImpl f = HttpClientFactoryImpl.builder()
                    .setSupportInsecureHttp(true).setSocketTimeoutSeconds(1).build();
            B2WebApiHttpClientImpl c = B2WebApiHttpClientImpl.builder().setHttpClientFactory(f).build();
            long start = System.nanoTime();
            try {
                c.getContent("http://localhost:" + ss.getLocalPort() + "/x", null, B2ContentMemoryWriter.build());
                fail("expected timeout");
            } catch (Exception e) {
                // expected
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            assertTrue("took " + ms + "ms, configured socket timeout 1s", ms < 5000);
        }
    }
}
