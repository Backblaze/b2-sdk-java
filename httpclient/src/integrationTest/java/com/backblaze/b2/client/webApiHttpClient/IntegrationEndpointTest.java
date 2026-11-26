/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Pure string/URI checks; no network and no environment needed. */
public class IntegrationEndpointTest {
    @Test
    public void testLoopbackHttpFormsAreAccepted() {
        assertTrue(IntegrationEndpoint.isLoopbackHttp("http://127.0.0.1"));
        assertTrue(IntegrationEndpoint.isLoopbackHttp("http://127.0.0.1:8080"));
        assertTrue(IntegrationEndpoint.isLoopbackHttp("http://127.0.0.1:8080/"));
        assertTrue(IntegrationEndpoint.isLoopbackHttp("http://localhost"));
        assertTrue(IntegrationEndpoint.isLoopbackHttp("http://localhost:9000/path"));
        assertTrue(IntegrationEndpoint.isLoopbackHttp("HTTP://LOCALHOST:9000"));
        assertTrue(IntegrationEndpoint.isLoopbackHttp("http://[::1]"));
        assertTrue(IntegrationEndpoint.isLoopbackHttp("http://[::1]:8080"));
    }

    @Test
    public void testLookalikeHostsAreRejected() {
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://localhost.evil.example"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://localhost.evil.example:8080"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://127.0.0.1.attacker.com"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://127.0.0.10"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://[::1]evil.example"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://localhostevil.example"));
    }

    @Test
    public void testUserinfoTricksAreRejected() {
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://127.0.0.1@evil.example"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://localhost:80@evil.example"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://user@127.0.0.1"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://127.0.0.1\\@evil.example"));
    }

    @Test
    public void testNonHttpSchemesAndGarbageAreRejected() {
        assertFalse(IntegrationEndpoint.isLoopbackHttp("https://localhost"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("ftp://localhost"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("localhost"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("http://local host"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp("not a url %%"));
        assertFalse(IntegrationEndpoint.isLoopbackHttp(""));
    }

    @Test
    public void testHttpsDetection() {
        assertTrue(IntegrationEndpoint.isHttps("https://api.backblazeb2.com"));
        assertTrue(IntegrationEndpoint.isHttps("HTTPS://api.backblazeb2.com"));
        assertFalse(IntegrationEndpoint.isHttps("http://api.backblazeb2.com"));
        assertFalse(IntegrationEndpoint.isHttps("https://"));
        assertFalse(IntegrationEndpoint.isHttps("https://user@/x"));
        assertFalse(IntegrationEndpoint.isHttps("nonsense"));
    }
}
