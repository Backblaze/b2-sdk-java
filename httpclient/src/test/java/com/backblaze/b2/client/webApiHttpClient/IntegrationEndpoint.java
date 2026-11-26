/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Decides whether the integration-test endpoint may be reached over plain http.
 */
final class IntegrationEndpoint {
    private IntegrationEndpoint() {
    }

    /**
     * True only for an http URL whose host is exactly 127.0.0.1, localhost or ::1, with no
     * userinfo. Anything unparseable is not loopback (fail closed).
     */
    static boolean isLoopbackHttp(String endpoint) {
        final URI uri = parse(endpoint);
        if (uri == null || !"http".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null) {
            return false;
        }
        final String host = uri.getHost();
        return host != null
                && ("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host) || "[::1]".equals(host));
    }

    /** True only for a well-formed https URL that has a host. */
    static boolean isHttps(String endpoint) {
        final URI uri = parse(endpoint);
        return uri != null
                && "https".equalsIgnoreCase(uri.getScheme())
                && uri.getUserInfo() == null
                && uri.getHost() != null;
    }

    private static URI parse(String endpoint) {
        try {
            return new URI(endpoint);
        } catch (URISyntaxException | NullPointerException e) {
            return null;
        }
    }
}
