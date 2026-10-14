// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.B2ClientConfig;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.webApiHttpClient.B2StorageHttpClientBuilder;
import com.backblaze.b2.client.webApiHttpClient.HttpClientFactory;
import com.backblaze.b2.client.webApiHttpClient.HttpClientFactoryImpl;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpCoreContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared plumbing for the sdkharness resilience checks. Compiled together with one check class by
 * ../run-resilience (a JDK 11 single-file launch cannot import a second source file).
 *
 * <p>Each check is one {@code main} that calls {@link #run}. A check arms one fault through the
 * simulator's control listener, makes one SDK call, and asserts both the end state (the bytes read
 * back, the listing, the error) and the recovery path, which it reads from the simulator's request
 * journal ({@code GET /journal}: sequence, endpoint, status, fault, an opaque upload URL id; never a
 * header, token or body). {@link #run} prints exactly one result line,
 * {@code RESILIENCE b2-sdk-java <scenario> @simulator: PASS | FAIL (<step> -- <detail>)}, after it
 * has cleared every fault and tried to delete every bucket and file the check created; a step that
 * fails turns the verdict into a FAIL. Anything else it prints is a {@code NOTE} line. Its own lines
 * are one capped printable line ({@link #tidy}) built from exception class names, status codes and the
 * check's own assertions, never from a response body; this is formatting, not redaction (the only
 * credential is the simulator's fixed test one).
 *
 * <p>Every SDK request goes through {@link #guardedFactory}, which refuses any request whose target
 * host is not 127.0.0.1 and fails the verdict if one was attempted. {@code GuardSelfTest} proves it.
 */
public final class Support {
    static final String SLUG = "b2-sdk-java";
    // The simulator's fixed test credential. No other credential is ever used.
    private static final String KEY_ID = "test-key-id";
    private static final String KEY = "test-key";
    private static final String LOOPBACK = "http://127\\.0\\.0\\.1:[0-9]{1,5}/?";

    /** The body of one check. */
    interface Body {
        void run() throws Exception;
    }

    /** One SDK call whose duration the check wants to observe. */
    interface Call {
        void run() throws Exception;
    }

    /** A failed assertion; the step is whatever {@link #step} last named. */
    static final class Failure extends RuntimeException {
        Failure(String detail) {
            super(detail);
        }
    }

    /** One line of the simulator's request journal. */
    static final class Entry {
        final String endpoint;
        /** The HTTP status, or -1 when the simulator closed the connection before answering. */
        final int status;
        /** The fault that fired on this request ("injected", "stall", "reset-mid-request", ...), or "". */
        final String fault;
        /** The opaque upload (or part) URL id the request used, or "". */
        final String uploadUrlId;

        Entry(String endpoint, int status, String fault, String uploadUrlId) {
            this.endpoint = endpoint;
            this.status = status;
            this.fault = fault;
            this.uploadUrlId = uploadUrlId;
        }

        @Override
        public String toString() {
            return endpoint + ":" + (status < 0 ? "none" : String.valueOf(status))
                    + (fault.isEmpty() ? "" : "(" + fault + ")") + (uploadUrlId.isEmpty() ? "" : "@" + uploadUrlId);
        }
    }

    static B2StorageClient client;
    static String bucketId;
    static String bucketName;

    private static String control;
    private static final HttpClient http = HttpClient.newHttpClient();
    private static String scenario = "unknown";
    private static String step = "configuration";
    private static final List<B2Bucket> created = new ArrayList<>();
    private static int journalMark;
    private static final String LOOPBACK_HOST = "127.0.0.1";
    /** Hosts of requests the guard refused (the SDK may swallow the exception, so the verdict reads this). */
    static final List<String> blocked = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> cleanupErrors = new ArrayList<>();

    private Support() {
    }

    // ---- the harness around one check ----------------------------------------------------------

    /** Runs a check with the SDK's default HTTP settings. */
    static void run(String id, Body body) {
        run(id, 0, body);
    }

    /**
     * Runs a check. {@code socketTimeoutSeconds} above zero sets the SDK's documented per-request
     * socket timeout (HttpClientFactoryImpl.Builder.setSocketTimeoutSeconds, whole seconds); zero
     * leaves the default.
     */
    static void run(String id, int socketTimeoutSeconds, Body body) {
        scenario = id;
        String failure = null;
        try {
            final String url = System.getenv("SDKHARNESS_SIMULATOR_URL");
            control = System.getenv("SDKHARNESS_SIMULATOR_CONTROL_URL");
            // run-resilience checked both already; the guard in newClient enforces it again on every SDK request.
            if (url == null || !url.matches(LOOPBACK) || control == null || !control.matches(LOOPBACK)) {
                throw new Failure("the simulator and control URLs must be IPv4 loopback HTTP origins");
            }
            control = control.replaceAll("/$", "");
            client = newClient(url, socketTimeoutSeconds);
            body.run();
        } catch (Failure f) {
            failure = step + " -- " + tidy(f.getMessage());
        } catch (Throwable t) {
            failure = step + " -- " + describe(t);
        } finally {
            cleanup();
        }
        if (!blocked.isEmpty()) {
            failure = "loopback guard -- blocked a request to non-loopback host " + tidy(blocked.get(0));
        } else if (failure == null && !cleanupErrors.isEmpty()) {
            failure = "cleanup -- " + cleanupErrors.size() + " step(s) failed: " + tidy(String.join("; ", cleanupErrors));
        }
        if (failure == null) {
            System.out.println("RESILIENCE " + SLUG + " " + scenario + " @simulator: PASS");
        } else {
            System.out.println("RESILIENCE " + SLUG + " " + scenario + " @simulator: FAIL (" + failure + ")");
            System.exit(1);
        }
    }

    private static B2StorageClient newClient(String masterUrl, int socketTimeoutSeconds) {
        final B2ClientConfig config = B2ClientConfig.builder(KEY_ID, KEY, "b2-sdk-java-sdkharness-resilience")
                .setMasterUrl(masterUrl)
                .build();
        // The SDK's own request settings (timeouts, no URI normalization), with the one setting a check
        // may change; the SDK's own retry policy and every other default stay as shipped. The guarded
        // factory below supplies plain HTTP for the simulator, the SDK's documented test-environment switch.
        final HttpClientFactoryImpl.Builder httpBuilder = HttpClientFactoryImpl.builder().setSupportInsecureHttp(true);
        if (socketTimeoutSeconds > 0) {
            httpBuilder.setSocketTimeoutSeconds(socketTimeoutSeconds);
        }
        return B2StorageHttpClientBuilder.builder(config)
                .setHttpClientFactory(guardedFactory(httpBuilder.createRequestConfig())).build();
    }

    /**
     * The one HTTP client factory every check uses. It mirrors HttpClientFactoryImpl (same request
     * config, pooled connections revalidated after 2 s, 4 s keep-alive) with plain HTTP only, and an
     * interceptor that refuses any request whose target host is not 127.0.0.1 before it is sent,
     * including each hop of a redirect. A refused request is recorded in {@link #blocked}, which fails
     * the verdict even if the SDK handles the exception.
     */
    static HttpClientFactory guardedFactory(RequestConfig requestConfig) {
        final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new PlainConnectionSocketFactory()).build();
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(100);
        connectionManager.setValidateAfterInactivity(2000);
        return new HttpClientFactory() {
            @Override
            public CloseableHttpClient create() {
                return HttpClients.custom()
                        .setConnectionManager(connectionManager)
                        .setConnectionManagerShared(true)
                        .setDefaultRequestConfig(requestConfig)
                        .setKeepAliveStrategy((response, context) -> 4000)
                        .addInterceptorFirst((HttpRequestInterceptor) (request, context) ->
                                requireLoopback(request, (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST)))
                        .build();
            }

            @Override
            public void close() {
                connectionManager.shutdown();
            }
        };
    }

    /**
     * Throws, and records the host, unless the request's target is 127.0.0.1. The target host is the
     * one the client is about to connect to (set on the context before interceptors run, also on each
     * redirect hop); an absolute request URI is checked as well, and a missing target fails closed.
     */
    static void requireLoopback(org.apache.http.HttpRequest request, HttpHost target) throws HttpException {
        String host = target == null ? null : target.getHostName();
        if (LOOPBACK_HOST.equals(host)) {
            final String uri = request.getRequestLine().getUri();
            try {
                final URI parsed = URI.create(uri);
                if (parsed.isAbsolute()) {
                    host = parsed.getHost();
                }
            } catch (IllegalArgumentException e) {
                host = "unparseable-uri";
            }
        }
        if (!LOOPBACK_HOST.equals(host)) {
            blocked.add(String.valueOf(host));
            throw new HttpException("blocked non-loopback host: " + host);
        }
    }

    /**
     * Clears every fault still armed (so the cleanup calls below cannot trip one), then deletes every
     * bucket and file this check created, on the error path too.
     */
    private static void cleanup() {
        try {
            if (control != null) {
                send("DELETE", "/faults", null);
            }
        } catch (Exception e) {
            cleanupErrors.add("clearing the faults: " + describe(e));
        }
        if (client == null) {
            return;
        }
        for (B2Bucket bucket : created) {
            try {
                client.deleteAllFilesInBucket(bucket.getBucketId());
                client.deleteBucket(bucket.getBucketId());
            } catch (B2Exception | RuntimeException e) {
                cleanupErrors.add(bucket.getBucketName() + ": " + describe(e));
            }
        }
        try {
            client.close();
        } catch (RuntimeException e) {
            // nothing left to release
        }
    }

    // ---- reporting ------------------------------------------------------------------------------

    static void step(String name) {
        step = name;
    }

    static void note(String text) {
        System.out.println("NOTE " + SLUG + " " + scenario + ": " + tidy(text));
    }

    static void fail(String detail) {
        throw new Failure(detail);
    }

    static void check(boolean ok, String detail) {
        if (!ok) {
            fail(detail);
        }
    }

    /** One line, printable, capped: a detail never carries a response body or a multi-line diagnostic. */
    static String tidy(String s) {
        final String line = String.valueOf(s).replaceAll("\\s+", " ").replaceAll("[^\\x20-\\x7E]", "?");
        return line.length() > 200 ? line.substring(0, 200) : line;
    }

    static String describe(Throwable t) {
        final StringBuilder sb = new StringBuilder(t.getClass().getSimpleName());
        if (t instanceof B2Exception) {
            sb.append(" status=").append(((B2Exception) t).getStatus()).append(" code=").append(((B2Exception) t).getCode());
        } else if (t.getMessage() != null) {
            sb.append(' ').append(t.getMessage());
        }
        return tidy(sb.toString());
    }

    // ---- the simulator's control listener ------------------------------------------------------

    private static String send(String method, String path, String json) throws Exception {
        final URI uri = URI.create(control + path);
        if (!LOOPBACK_HOST.equals(uri.getHost())) {
            blocked.add(String.valueOf(uri.getHost()));
            fail("the control request would leave loopback");
        }
        final HttpRequest.Builder request = HttpRequest.newBuilder(uri);
        if (json == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(json));
        }
        final HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            fail("the control listener answered " + response.statusCode() + " to " + method + " " + path);
        }
        return response.body();
    }

    /** Arms one status fault on {@code endpoint}: {@code POST /faults {on, status, code, retryAfter?, count: 1}}. */
    static void armFault(String endpoint, int status, String code, Integer retryAfterSeconds) throws Exception {
        send("POST", "/faults", "{\"on\":\"" + endpoint + "\",\"status\":" + status + ",\"code\":\"" + code + "\""
                + (retryAfterSeconds == null ? "" : ",\"retryAfter\":" + retryAfterSeconds) + ",\"count\":1}");
    }

    /** Arms one connection fault on {@code endpoint}: {@code POST /wire-faults {on, kind, ms?, count: 1}}. */
    static void armWireFault(String endpoint, String kind, Integer ms) throws Exception {
        send("POST", "/wire-faults", "{\"on\":\"" + endpoint + "\",\"kind\":\"" + kind + "\""
                + (ms == null ? "" : ",\"ms\":" + ms) + ",\"count\":1}");
    }

    /** Moves the simulator's clock forward: {@code POST /clock {advanceMs}}. */
    static void advanceClock(long ms) throws Exception {
        send("POST", "/clock", "{\"advanceMs\":" + ms + "}");
    }

    private static final Pattern ENTRY = Pattern.compile("\\{[^{}]*\"endpoint\"[^{}]*\\}");

    private static List<Entry> journal() throws Exception {
        final List<Entry> entries = new ArrayList<>();
        final Matcher m = ENTRY.matcher(send("GET", "/journal", null));
        while (m.find()) {
            final String o = m.group();
            entries.add(new Entry(text(o, "endpoint"), number(o, "status"), text(o, "fault"), text(o, "uploadUrlId")));
        }
        return entries;
    }

    private static String text(String object, String key) {
        final Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(null|\"([^\"]*)\")").matcher(object);
        return m.find() && m.group(2) != null ? m.group(2) : "";
    }

    private static int number(String object, String key) {
        final Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?[0-9]+)").matcher(object);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** Remembers where the journal stands, so {@link #sinceMark} returns only what the call under test caused. */
    static void markJournal() throws Exception {
        journalMark = journal().size();
    }

    /** The requests the simulator saw since {@link #markJournal}, in order. */
    static List<Entry> sinceMark() throws Exception {
        final List<Entry> all = journal();
        check(all.size() >= journalMark, "the journal shrank (" + all.size() + " entries after " + journalMark + ")");
        return new ArrayList<>(all.subList(journalMark, all.size()));
    }

    static List<Entry> only(List<Entry> entries, String endpoint) {
        final List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e.endpoint.equals(endpoint)) {
                out.add(e);
            }
        }
        return out;
    }

    static String show(List<Entry> entries) {
        final StringBuilder sb = new StringBuilder();
        for (Entry e : entries) {
            sb.append(sb.length() == 0 ? "" : " ").append(e);
        }
        return tidy(sb.toString());
    }

    /** The index of the first entry at or after {@code from} on {@code endpoint}, or -1. */
    static int indexOf(List<Entry> entries, int from, String endpoint) {
        for (int i = from; i < entries.size(); i++) {
            if (entries.get(i).endpoint.equals(endpoint)) {
                return i;
            }
        }
        return -1;
    }

    // ---- fixtures -------------------------------------------------------------------------------

    /** A fresh private bucket named sdkharness-res-<random>, deleted (with its files) on exit. */
    static B2Bucket bucket() throws B2Exception {
        final B2Bucket bucket = client.createBucket(
                "sdkharness-res-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12), "allPrivate");
        created.add(bucket);
        bucketId = bucket.getBucketId();
        bucketName = bucket.getBucketName();
        return bucket;
    }

    static B2FileVersion upload(String name, byte[] data) throws B2Exception {
        return client.uploadSmallFile(B2UploadFileRequest
                .builder(bucketId, name, "application/octet-stream", B2ByteArrayContentSource.build(data)).build());
    }

    /** Uploads {@code data} as a large file in several parts, two at a time. */
    static B2FileVersion uploadLarge(String name, byte[] data) throws B2Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            return client.uploadLargeFile(B2UploadFileRequest
                    .builder(bucketId, name, "application/octet-stream", B2ByteArrayContentSource.build(data)).build(), executor);
        } finally {
            executor.shutdown();
        }
    }

    /** Deterministic bytes that cover all 256 values, so a text-only path cannot pass. */
    static byte[] payload(int length, long seed) {
        final byte[] data = new byte[length];
        new Random(seed).nextBytes(data);
        for (int i = 0; i < Math.min(256, length); i++) {
            data[i] = (byte) i;
        }
        return data;
    }

    static byte[] downloadById(String fileId) throws B2Exception {
        final B2ContentMemoryWriter writer = B2ContentMemoryWriter.build();
        client.downloadById(fileId, writer);
        return writer.getBytes();
    }

    static byte[] downloadByName(String name) throws B2Exception {
        final B2ContentMemoryWriter writer = B2ContentMemoryWriter.build();
        client.downloadByName(bucketName, name, writer);
        return writer.getBytes();
    }

    /** Every version of {@code name} in the check's bucket, as the listing returns them. */
    static List<B2FileVersion> versionsOf(String name) throws B2Exception {
        final List<B2FileVersion> all = new ArrayList<>();
        for (B2FileVersion v : client.fileVersions(B2ListFileVersionsRequest.builder(bucketId).setPrefix(name).build())) {
            if (v.getFileName().equals(name)) {
                all.add(v);
            }
        }
        return all;
    }

    static List<String> listNames() throws B2Exception {
        final List<String> names = new ArrayList<>();
        for (B2FileVersion v : client.fileNames(B2ListFileNamesRequest.builder(bucketId).build())) {
            names.add(v.getFileName());
        }
        return names;
    }

    /** Reads {@code name} back by name and requires it to be byte-for-byte {@code expected}. */
    static void requireIntact(String name, byte[] expected) throws B2Exception {
        final byte[] got = downloadByName(name);
        check(Arrays.equals(expected, got),
                name + " read back as " + got.length + " bytes that differ from the " + expected.length + " uploaded");
    }

    static String sha1(byte[] data) {
        try {
            final StringBuilder hex = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-1").digest(data)) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Runs {@code call} and returns how long it took, in milliseconds (a lower bound is all a check may assert). */
    static long timed(Call call) throws Exception {
        final long start = System.nanoTime();
        call.run();
        return (System.nanoTime() - start) / 1_000_000;
    }

    // ---- shared assertions ---------------------------------------------------------------------

    /**
     * The upload recovery path every {@code upload.*} status and wire fault shares: after the faulted
     * {@code b2_upload_file} the SDK asks for a fresh upload URL ({@code b2_get_upload_url}), and the
     * retried {@code b2_upload_file} succeeds on a different URL id.
     */
    static void requireFreshUrlRetry(String fault, List<Entry> window) {
        final int hit = indexOf(window, 0, "b2_upload_file");
        check(hit >= 0 && window.get(hit).fault.equals(fault),
                "the first b2_upload_file did not carry the " + fault + " fault: " + show(window));
        final int again = indexOf(window, hit + 1, "b2_get_upload_url");
        check(again >= 0, "no fresh b2_get_upload_url after the faulted upload: " + show(window));
        final int retry = indexOf(window, again + 1, "b2_upload_file");
        check(retry >= 0, "no second b2_upload_file after the fresh upload URL: " + show(window));
        final Entry first = window.get(hit);
        final Entry second = window.get(retry);
        check(second.status == 200 && second.fault.isEmpty(), "the retried upload did not succeed: " + show(window));
        check(!second.uploadUrlId.isEmpty() && !second.uploadUrlId.equals(first.uploadUrlId),
                "the retry reused the faulted upload URL (" + first.uploadUrlId + "): " + show(window));
    }
}
