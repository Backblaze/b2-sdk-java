// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.webApiHttpClient.HttpClientFactory;
import com.backblaze.b2.client.webApiHttpClient.HttpClientFactoryImpl;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Proves the loopback guard in Support. Not a harness scenario (it has no row in tests.tsv); run by
 * hand, see ../../README.md. Needs no simulator: it starts its own 127.0.0.1 server.
 *
 * <p>It checks that a request to a loopback server passes, that an absolute request URI to a
 * non-loopback host (a documentation address, never contacted) is refused before it is sent, and that
 * a loopback server redirecting to a non-loopback host is refused on the redirect hop.
 */
public final class GuardSelfTest {
    private GuardSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/ok", ex -> {
            ex.sendResponseHeaders(200, -1);
            ex.close();
        });
        server.createContext("/redirect", ex -> {
            ex.getResponseHeaders().add("Location", "http://203.0.113.7:9/elsewhere");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        server.start();
        final String base = "http://127.0.0.1:" + server.getAddress().getPort();
        final HttpClientFactory factory = Support.guardedFactory(
                HttpClientFactoryImpl.builder().setSupportInsecureHttp(true).createRequestConfig());
        String problem = null;
        try (CloseableHttpClient client = factory.create()) {
            try (CloseableHttpResponse r = client.execute(new HttpGet(base + "/ok"))) {
                if (r.getStatusLine().getStatusCode() != 200) {
                    problem = "a loopback request did not reach the server";
                }
            }
            if (problem == null && !Support.blocked.isEmpty()) {
                problem = "a loopback request was blocked";
            }
            if (problem == null) {
                problem = mustBeBlocked(client, new HttpGet("http://203.0.113.7:9/absolute"), "an absolute non-loopback URL", "203.0.113.7");
            }
            if (problem == null) {
                problem = mustBeBlocked(client, new HttpGet(base + "/redirect"), "a redirect to a non-loopback host", "203.0.113.7");
            }
            if (problem == null) {
                problem = mustBeBlocked(client, new HttpGet("http://localhost:" + server.getAddress().getPort() + "/ok"),
                        "a loopback name that is not 127.0.0.1", "localhost");
            }
        } catch (Exception e) {
            problem = "unexpected " + Support.describe(e);
        } finally {
            factory.close();
            server.stop(0);
        }
        if (problem == null) {
            System.out.println("GUARD-SELFTEST " + Support.SLUG + ": PASS");
        } else {
            System.out.println("GUARD-SELFTEST " + Support.SLUG + ": FAIL (" + Support.tidy(problem) + ")");
            System.exit(1);
        }
    }

    private static String mustBeBlocked(CloseableHttpClient client, HttpGet get, String what, String host) {
        final int before = Support.blocked.size();
        try (CloseableHttpResponse r = client.execute(get)) {
            return what + " was sent (status " + r.getStatusLine().getStatusCode() + ")";
        } catch (Exception e) {
            if (Support.blocked.size() != before + 1 || !host.equals(Support.blocked.get(before))) {
                return what + " failed without being blocked by the guard: " + Support.describe(e);
            }
            return null;
        }
    }
}
