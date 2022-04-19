/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.util.B2Preconditions;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.VersionInfo;

import javax.net.ssl.SSLContext;
import java.util.concurrent.TimeUnit;

/**
 * This is the default HttpClientFactory implementation.
 *
 * Created HttpClients always support 'https' because that's what the
 * production B2 servers require.  By default, the created HttpClients
 * do not support 'http' to ensure we don't send data over http by accident.
 *
 * If you have a non-https implementation of B2 that you test against,
 * you *may* choose to enable 'http' support when creating the factory.
 * We really do *not* recommend that in production.
 */
public class HttpClientFactoryImpl implements HttpClientFactory {
    private final HttpClientConnectionManager connectionManager;
    private final RequestConfig requestConfig;
    private final IdleConnectionMonitorThread connectionJanitor;

    /**
     * This is the user-agent we should use on Apache HttpClient instances.
     * If we do not set it, HttpClientBuilder will compute this every time
     * it creates an HttpClient (and we do that A LOT).  That wouldn't
     * be so bad, except that internally it gets its own version by opening
     * a resource stream which involves opening a jar and using a ZipFile
     * instance, etc, so it's a non-obvious amount of work.  (at least as
     * of httpcomponents-client-4.5.2).  So, we do the work once and
     * manually set the userAgent from the resulting constant.
     */
    private static final String APACHE_HTTP_CLIENT_USER_AGENT = VersionInfo.getUserAgent("Apache-HttpClient",
            "org.apache.http.client", HttpClientBuilder.class);


    private HttpClientFactoryImpl(HttpClientConnectionManager connectionManager,
                          RequestConfig requestConfig) {
        this.connectionManager = connectionManager;
        this.requestConfig = requestConfig;
        connectionJanitor = new IdleConnectionMonitorThread(connectionManager);
        connectionJanitor.start();
    }

    @SuppressWarnings("WeakerAccess")
    public static HttpClientFactoryImpl build() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CloseableHttpClient create() throws B2Exception {
        return HttpClients.custom()
                .setUserAgent(APACHE_HTTP_CLIENT_USER_AGENT)
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    @Override
    public void close() {
        connectionManager.shutdown();
        connectionJanitor.shutdown();
        try {
            connectionJanitor.join();
        } catch (InterruptedException e) {
            // restore the interrupt because we're not acting on it here.
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The factory we're building will have close() called on it and when it
     * does, it will close its connection manager.  Since we don't want to
     * close a connection manager out from under another factory, each Builder
     * is only allowed to execute build() once.
     */
    public static class Builder {
        private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT_SECONDS = 5;
        private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
        private static final int DEFAULT_SOCKET_TIMEOUT_SECONDS = 20;

        private static final int DEFAULT_MAX_TOTAL_CONNECTIONS_IN_POOL = 100;
        private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 100;

        private boolean builtOneAlready;

        // should the clients support 'http'?  (they always support 'https'.)
        // this is off by default, and that's a good way to leave it.
        // http is only supported for use with some test environments.
        private boolean supportInsecureHttp;

        // for RequestConfig
        private int connectionRequestTimeoutSeconds = DEFAULT_CONNECTION_REQUEST_TIMEOUT_SECONDS;
        private int connectTimeoutSeconds = DEFAULT_CONNECT_TIMEOUT_SECONDS;
        private int socketTimeoutSeconds = DEFAULT_SOCKET_TIMEOUT_SECONDS;

        // for connection pool
        private int maxTotalConnectionsInPool = DEFAULT_MAX_TOTAL_CONNECTIONS_IN_POOL;
        private int maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;

        public Builder setSupportInsecureHttp(boolean supportInsecureHttp) {
            this.supportInsecureHttp = supportInsecureHttp;
            return this;
        }

        public Builder setConnectionRequestTimeoutSeconds(int connectionRequestTimeoutSeconds) {
            this.connectionRequestTimeoutSeconds = connectionRequestTimeoutSeconds;
            return this;
        }

        public Builder setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
            return this;
        }

        public Builder setSocketTimeoutSeconds(int socketTimeoutSeconds) {
            this.socketTimeoutSeconds = socketTimeoutSeconds;
            return this;
        }

        public Builder setMaxTotalConnectionsInPool(int maxTotalConnectionsInPool) {
            this.maxTotalConnectionsInPool = maxTotalConnectionsInPool;
            return this;
        }

        public Builder setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
            return this;
        }


        public HttpClientFactoryImpl build() {
            B2Preconditions.checkState(!builtOneAlready, "called build() more than once?!");
            builtOneAlready = true;

            return new HttpClientFactoryImpl(
                    createConnectionManager(),
                    createRequestConfig());
        }

        public RequestConfig createRequestConfig() {
            return RequestConfig.custom()
                    .setConnectionRequestTimeout(connectionRequestTimeoutSeconds * 1000) // time waiting for cxn from pool
                    .setConnectTimeout(connectTimeoutSeconds * 1000) // time waiting for remote server to connect
                    .setSocketTimeout(socketTimeoutSeconds * 1000) // time waiting for answer after connecting
                    // don't try to normalize URIs, like convert '//' to '/'; this would change filenames for
                    // DownloadByName requests
                    .setNormalizeUri(false)
                    .build();

        }

        private HttpClientConnectionManager createConnectionManager() {
            // For SSL/TLS,
            //   HttpClient says it uses Java Secure Socket Extension:
            //     https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
            //   java 8 defaults to TLS/1.2.
            //     https://blogs.oracle.com/java-platform-group/jdk-8-will-use-tls-12-as-default
            //   for hostname verification,
            //     HttpClient's DefaultHostnameVerifier is fine and allows wildcard names.
            //     i've seen some code use ALLOW_ALL_HOSTNAME_VERIFIER, but that's deprecated and bad.
            //
            // we are NOT using the default registry because the default supports http
            // and we usually don't want to suport http.
            //
            // This code is based on https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html

            RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();

            // we *always* support https, since that's what the official b2 servers require.
            {
                SSLContext sslcontext = SSLContexts.createDefault();
                ConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(sslcontext);
                registryBuilder.register("https", sslFactory);
            }

            if (supportInsecureHttp) {
                ConnectionSocketFactory plainFactory = new PlainConnectionSocketFactory();
                registryBuilder.register("http", plainFactory);
            }

            final Registry<ConnectionSocketFactory> registry = registryBuilder.build();

            final PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager(registry);
            mgr.setMaxTotal(maxTotalConnectionsInPool);
            mgr.setDefaultMaxPerRoute(maxConnectionsPerRoute);
            return mgr;
        }
    }

    // from https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
    private static class IdleConnectionMonitorThread extends Thread {

        private final HttpClientConnectionManager connMgr;
        private volatile boolean shutdown;

        IdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(5000);
                        // Close expired connections
                        connMgr.closeExpiredConnections();
                        // Optionally, close connections
                        // that have been idle longer than 30 sec
                        connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }

    }

}
