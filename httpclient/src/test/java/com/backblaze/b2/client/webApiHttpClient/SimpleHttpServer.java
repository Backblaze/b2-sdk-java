/*
 * Copyright 2021, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple local http server
 *
 * (handle GET requests for now)
 */
public class SimpleHttpServer implements Cloneable {

    /* Wrapper for a SimpleHttpRequestHandler that will handle actual requests */
    private static class HandlerWrapper implements HttpHandler {
        private final SimpleHttpRequestHandler handler;

        public HandlerWrapper(SimpleHttpRequestHandler handler) {
            this.handler = handler;
        }

        /**
         * handle incoming http request
         * @param httpExchange incoming Http request
         * @throws IOException
         */
        public void handle(HttpExchange httpExchange) throws IOException {
            // Get the body of the request, or null if there isn't one.
            byte [] requestBody = null;
            final String requestMethod = httpExchange.getRequestMethod();
            switch(requestMethod) {
                case "GET": {
                    String query = httpExchange.getRequestURI().getQuery();
                    if (query == null) {
                        query = "";
                    }
                    requestBody = query.getBytes();
                    break;
                }
                default:
                    break;
            }

            // Gather the headers into a map, to make them easy to access.
            // This will lose duplicate headers for the same key, but it
            // wouldn't matter since the data sent have only one value mapped to one key
            final Map<String, String> headerMap = new HashMap<>();
            final Headers headers = httpExchange.getRequestHeaders();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                for (String value : entry.getValue()) {
                    headerMap.put(entry.getKey(), value);
                }
            }

            // Call the handler and write the response.
            final SimpleHttpRequestHandler.Response response = handler.handleRequest(requestMethod, headerMap, requestBody);
            final byte [] responseBytes = response.data;
            final Headers responseHeaders = httpExchange.getResponseHeaders();

            // fill response header
            responseHeaders.clear();
            if (response.headers != null) {
                response.headers.forEach(responseHeaders::add);
            }

            httpExchange.sendResponseHeaders(response.statusCode, responseBytes.length);
            final OutputStream out = httpExchange.getResponseBody();
            out.write(responseBytes);
            out.close();
        }
    }

    /**
     * HttpHandler that always returns a 404.
     */
    private static class Handler404 implements HttpHandler {
        public void handle(HttpExchange httpExchange) throws IOException {
            httpExchange.sendResponseHeaders(404, 0);
            httpExchange.getResponseBody().close();
        }
    }

    private final HttpServer server;
    private final int portNumber;

    /**
     * Instantiate a web server on the specified port and backlog.
     *
     * @param portNumber the port to bind to
     * @param backlog the maximum number of queued incoming connections to
     *        allow on the listening socket
     */
    public SimpleHttpServer(int portNumber, int backlog) {
        try {
            final InetSocketAddress address = new InetSocketAddress(portNumber);
            this.server = HttpServer.create(address, backlog);
            this.server.createContext("/", new Handler404());
            this.server.start();
            this.portNumber = server.getAddress().getPort();
        } catch (IOException e) {
            throw new RuntimeException("Could not create HTTP server", e);
        }
    }

    /**
     * return the socket port number
     *
     * @return the port number
     */
    public int getPort() {
        return this.portNumber;
    }

    /**
     * Registers a handler to receive requests at a given path.  Any request whose
     * path is prefixed with the given path will be given to the handler.
     *
     * @param path A URI path, such as "/api/foo"
     * @param handler The SimpleHttpRequestHandler that will handle the requests.
     */
    public void addPath(String path, SimpleHttpRequestHandler handler) {
        server.createContext(path, new HandlerWrapper(handler));
    }

    /**
     * Stop the web server and clean up its resources.
     */
    public void close() {
        server.stop(0);
    }
}
