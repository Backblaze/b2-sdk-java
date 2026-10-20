// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.B2ClientConfig;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CreateBucketRequest;
import com.backblaze.b2.client.structures.B2DeleteFileVersionRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2LegalHold;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2UpdateFileLegalHoldRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.webApiHttpClient.B2StorageHttpClientBuilder;
import com.backblaze.b2.client.webApiHttpClient.HttpClientFactory;
import com.backblaze.b2.client.webApiHttpClient.HttpClientFactoryImpl;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared plumbing for the sdkharness conformance checks. Compiled together with one check class
 * by ../run-conformance (a JDK 11 single-file launch cannot import a second source file).
 *
 * <p>Each check is one {@code main} that calls {@link #run}. This prints exactly one result line,
 * {@code CONFORMANCE b2-sdk-java <capability> @simulator: PASS | FAIL (<step> -- <detail>)}, after
 * deleting every bucket and file the check created; a deletion that fails turns the verdict into a
 * FAIL. Anything else it prints is a {@code NOTE} line. Its own lines are one capped printable line
 * ({@link #tidy}) built from exception class names, status codes and the check's own assertions, never
 * from a response body; this is formatting, not redaction (the only credential is the simulator's fixed
 * test one).
 *
 * <p>Every SDK request goes through {@link #guardedFactory}, which refuses any request whose target
 * host is not 127.0.0.1 and fails the verdict if one was attempted. {@code GuardSelfTest} proves it.
 */
public final class Support {
    static final String SLUG = "b2-sdk-java";
    // The simulator's fixed test credential. No other credential is ever used.
    private static final String KEY_ID = "test-key-id";
    private static final String KEY = "test-key";

    /** The body of one check. */
    interface Body {
        void run() throws Exception;
    }

    /** One SDK call that is expected to throw. */
    interface Call {
        void run() throws Exception;
    }

    /** A failed assertion; the step is whatever {@link #step} last named. */
    static final class Failure extends RuntimeException {
        Failure(String detail) {
            super(detail);
        }
    }

    /** What a download delivered: the response headers and every byte. */
    static final class Downloaded {
        B2Headers headers;
        byte[] bytes;
    }

    static B2StorageClient client;
    /** "METHOD uri" of every HTTP request the SDK sent, when the check asked for the recording client. */
    static final List<String> wire = Collections.synchronizedList(new ArrayList<>());

    /**
     * With the recording client, requests whose URI contains {@link #inFlightMarker} are counted while they are in
     * flight (from the moment the SDK starts sending to the moment the response arrives); {@link #maxInFlight} is the
     * most that overlapped.
     */
    static volatile String inFlightMarker;
    static final AtomicInteger inFlight = new AtomicInteger();
    static final AtomicInteger maxInFlight = new AtomicInteger();

    private static final List<ExecutorService> executors = new ArrayList<>();
    private static String capability = "unknown";
    private static String step = "configuration";
    private static final List<B2Bucket> created = new ArrayList<>();
    private static final java.util.Set<String> lockEnabled = new java.util.HashSet<>();
    private static final String LOOPBACK_HOST = "127.0.0.1";
    /** Hosts of requests the guard refused (the SDK may swallow the exception, so the verdict reads this). */
    static final List<String> blocked = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> cleanupErrors = new ArrayList<>();

    private Support() {
    }

    // ---- the harness around one check ----------------------------------------------------------

    static void run(String cap, Body body) {
        run(cap, false, body);
    }

    static void run(String cap, boolean recordWire, Body body) {
        capability = cap;
        String failure = null;
        try {
            final String url = System.getenv("SDKHARNESS_SIMULATOR_URL");
            // run-conformance checked this already; the guard in newClient enforces it again on every request.
            if (url == null || !url.matches("http://127\\.0\\.0\\.1:[0-9]{1,5}/?")) {
                throw new Failure("simulator URL must be an IPv4 loopback HTTP origin");
            }
            client = newClient(url, recordWire);
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
            failure = "cleanup -- left " + cleanupErrors.size() + " bucket(s) behind: " + tidy(String.join("; ", cleanupErrors));
        }
        if (failure == null) {
            System.out.println("CONFORMANCE " + SLUG + " " + capability + " @simulator: PASS");
        } else {
            System.out.println("CONFORMANCE " + SLUG + " " + capability + " @simulator: FAIL (" + failure + ")");
            System.exit(1);
        }
    }

    /**
     * A second client, authorized with a key the check itself created (keys.crud), through the same guarded factory.
     * It is closed with the check. The credential is the caller's; nothing here prints it.
     */
    static B2StorageClient clientFor(String keyId, String key) {
        final B2StorageClient other = newClient(System.getenv("SDKHARNESS_SIMULATOR_URL"), keyId, key, false);
        extraClients.add(other);
        return other;
    }

    private static final List<B2StorageClient> extraClients = new ArrayList<>();

    private static B2StorageClient newClient(String masterUrl, boolean recordWire) {
        return newClient(masterUrl, KEY_ID, KEY, recordWire);
    }

    private static B2StorageClient newClient(String masterUrl, String keyId, String key, boolean recordWire) {
        final B2ClientConfig config = B2ClientConfig.builder(keyId, key, "b2-sdk-java-sdkharness-conformance")
                .setMasterUrl(masterUrl)
                .build();
        // The SDK's own request settings (timeouts, no URI normalization); the guarded factory below
        // supplies plain HTTP for the simulator, which is the SDK's documented test-environment switch.
        final RequestConfig requestConfig = HttpClientFactoryImpl.builder().setSupportInsecureHttp(true).createRequestConfig();
        return B2StorageHttpClientBuilder.builder(config).setHttpClientFactory(guardedFactory(requestConfig, recordWire)).build();
    }

    /**
     * The one HTTP client factory every check uses. It mirrors HttpClientFactoryImpl (same request
     * config, pooled connections revalidated after 2 s, 4 s keep-alive) with plain HTTP only, and an
     * interceptor that refuses any request whose target host is not 127.0.0.1 before it is sent,
     * including each hop of a redirect. A refused request is recorded in {@link #blocked}, which fails
     * the verdict even if the SDK handles the exception.
     */
    static HttpClientFactory guardedFactory(RequestConfig requestConfig, boolean recordWire) {
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
                        .addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                            requireLoopback(request, (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST));
                            if (recordWire) {
                                wire.add(request.getRequestLine().getMethod() + " " + request.getRequestLine().getUri());
                                if (matchesInFlight(request)) {
                                    final int now = inFlight.incrementAndGet();
                                    maxInFlight.accumulateAndGet(now, Math::max);
                                }
                            }
                        })
                        .addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                            final HttpRequest request = (HttpRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
                            if (recordWire && request != null && matchesInFlight(request)) {
                                inFlight.decrementAndGet();
                            }
                        })
                        .build();
            }

            @Override
            public void close() {
                connectionManager.shutdown();
            }
        };
    }

    private static boolean matchesInFlight(HttpRequest request) {
        final String marker = inFlightMarker;
        return marker != null && request.getRequestLine().getUri().contains(marker);
    }

    /**
     * Throws, and records the host, unless the request's target is 127.0.0.1. The target host is the
     * one the client is about to connect to (set on the context before interceptors run, also on each
     * redirect hop); an absolute request URI is checked as well, and a missing target fails closed.
     */
    static void requireLoopback(HttpRequest request, HttpHost target) throws HttpException {
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

    /** Tries to delete every file version and bucket this check created, on the error path too; a failure is collected, and {@link #run} turns it into a FAIL. */
    private static void cleanup() {
        if (client == null) {
            return;
        }
        for (ExecutorService executor : executors) {
            executor.shutdownNow();
        }
        for (B2StorageClient other : extraClients) {
            try {
                other.close();
            } catch (RuntimeException e) {
                // nothing left to release
            }
        }
        for (String keyId : createdKeys) {
            try {
                client.deleteKey(com.backblaze.b2.client.structures.B2DeleteKeyRequest.builder(keyId).build());
            } catch (B2Exception | RuntimeException e) {
                cleanupErrors.add("key " + keyId + ": " + describe(e));
            }
        }
        for (B2Bucket bucket : created) {
            try {
                // A large file left unfinished would keep the bucket from being deleted.
                for (B2FileVersion unfinished : client.unfinishedLargeFiles(bucket.getBucketId())) {
                    client.cancelLargeFile(unfinished.getFileId());
                }
                if (lockEnabled.contains(bucket.getBucketId())) {
                    // Files under a lock: a legal hold is released and governance retention is bypassed (the simulator's
                    // key holds that capability). Compliance retention is never set by a check, since nothing can lift it.
                    for (B2FileVersion version : client.fileVersions(bucket.getBucketId())) {
                        try {
                            client.updateFileLegalHold(B2UpdateFileLegalHoldRequest.builder(version.getFileName(), version.getFileId(), B2LegalHold.OFF).build());
                        } catch (B2Exception e) {
                            // a hide marker has no legal hold to release
                        }
                        client.deleteFileVersion(B2DeleteFileVersionRequest.builder(version.getFileName(), version.getFileId()).setBypassGovernance(true).build());
                    }
                } else {
                    client.deleteAllFilesInBucket(bucket.getBucketId());
                }
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
        System.out.println("NOTE " + SLUG + " " + capability + ": " + tidy(text));
    }

    static void fail(String detail) {
        throw new Failure(detail);
    }

    static void check(boolean ok, String detail) {
        if (!ok) {
            fail(detail);
        }
    }

    static void eq(String what, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            fail(what + ": expected " + expected + " but got " + actual);
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

    /** Runs {@code call}, which must fail with a B2 service error, and returns that error. */
    static B2Exception expectError(String what, Call call) {
        try {
            call.run();
        } catch (Failure f) {
            throw f;
        } catch (B2Exception e) {
            return e;
        } catch (Exception e) {
            fail(what + ": expected a B2 error but got " + describe(e));
        }
        fail(what + ": expected an error but the call succeeded");
        return null;
    }

    // ---- fixtures -------------------------------------------------------------------------------

    /** A fresh private bucket named sdkharness-conf-<random>, deleted (with its files) on exit. */
    static B2Bucket bucket() throws B2Exception {
        final B2Bucket bucket = client.createBucket(newBucketName(), "allPrivate");
        created.add(bucket);
        return bucket;
    }

    /** A fresh private bucket with Object Lock enabled; cleanup releases legal holds and bypasses governance retention. */
    static B2Bucket lockedBucket() throws B2Exception {
        return bucket(true, null);
    }

    /** A fresh private bucket made from a request the caller may customize; {@code lock} also enables Object Lock. */
    static B2Bucket bucket(boolean lock, java.util.function.Consumer<B2CreateBucketRequest.Builder> customize) throws B2Exception {
        final B2CreateBucketRequest.Builder builder = B2CreateBucketRequest.builder(newBucketName(), "allPrivate");
        if (lock) {
            builder.setFileLockEnabled(true);
        }
        if (customize != null) {
            customize.accept(builder);
        }
        final B2Bucket bucket = client.createBucket(builder.build());
        created.add(bucket);
        if (lock) {
            lockEnabled.add(bucket.getBucketId());
        }
        return bucket;
    }

    static String newBucketName() {
        return "sdkharness-conf-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static final List<String> createdKeys = new ArrayList<>();

    /** Records an application key the check created, so cleanup deletes it if the check does not. */
    static void trackKey(String keyId) {
        createdKeys.add(keyId);
    }

    /** The check deleted this key itself. */
    static void untrackKey(String keyId) {
        createdKeys.remove(keyId);
    }

    /** Records a bucket the check created itself, so cleanup deletes it if the check does not. */
    static void track(B2Bucket bucket) {
        created.add(bucket);
    }

    /** The check deleted this bucket itself, so cleanup has nothing left to do for it. */
    static void untrack(B2Bucket bucket) {
        created.removeIf(b -> b.getBucketId().equals(bucket.getBucketId()));
    }

    /** A thread pool the SDK's large-file calls run on; shut down when the check ends. */
    static ExecutorService executor(int threads) {
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        executors.add(executor);
        return executor;
    }

    /** The part size to use: the larger of the account's recommended and absolute minimum part size, as the realm reports them. */
    static long partSize() {
        try {
            final long recommended = client.getAccountAuthorization().getRecommendedPartSize();
            final long minimum = client.getAccountAuthorization().getAbsoluteMinimumPartSize();
            return Math.max(recommended, minimum);
        } catch (B2Exception e) {
            throw new Failure("could not read the account's part sizes: " + describe(e));
        }
    }

    /** How many recorded requests were "METHOD uri" with the given method and a URI containing {@code part}. */
    static int wireCount(String method, String part) {
        int n = 0;
        synchronized (wire) {
            for (String line : wire) {
                if (line.startsWith(method + " ") && line.contains(part)) {
                    n++;
                }
            }
        }
        return n;
    }

    static B2FileVersion upload(B2Bucket bucket, String name, byte[] data, String contentType,
                                Map<String, String> fileInfo) throws B2Exception {
        final B2UploadFileRequest.Builder request = B2UploadFileRequest
                .builder(bucket.getBucketId(), name, contentType, B2ByteArrayContentSource.build(data));
        if (fileInfo != null) {
            request.setCustomFields(fileInfo);
        }
        return client.uploadSmallFile(request.build());
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

    static Downloaded downloadById(String fileId) throws B2Exception {
        final Downloaded got = new Downloaded();
        client.downloadById(fileId, (headers, in) -> {
            got.headers = headers;
            got.bytes = in.readAllBytes();
        });
        return got;
    }

    /** Download by id with server-side-encryption request headers (an SSE-C key); {@code sse} may be null. */
    static Downloaded downloadById(String fileId, com.backblaze.b2.client.structures.B2FileSseForRequest sse) throws B2Exception {
        final Downloaded got = new Downloaded();
        client.downloadById(com.backblaze.b2.client.structures.B2DownloadByIdRequest.builder(fileId).setServerSideEncryption(sse).build(),
                (headers, in) -> {
                    got.headers = headers;
                    got.bytes = in.readAllBytes();
                });
        return got;
    }

    static Downloaded downloadByName(String bucketName, String fileName) throws B2Exception {
        final Downloaded got = new Downloaded();
        client.downloadByName(bucketName, fileName, (headers, in) -> {
            got.headers = headers;
            got.bytes = in.readAllBytes();
        });
        return got;
    }

    /** Every version (uploads and hide markers) whose name starts with {@code prefix}, as listed by B2. */
    static List<B2FileVersion> versions(String bucketId, String prefix) throws B2Exception {
        final List<B2FileVersion> all = new ArrayList<>();
        for (B2FileVersion version : client.fileVersions(B2ListFileVersionsRequest.builder(bucketId).setPrefix(prefix).build())) {
            all.add(version);
        }
        return all;
    }

    static List<String> names(Iterable<B2FileVersion> versions) {
        final List<String> names = new ArrayList<>();
        for (B2FileVersion version : versions) {
            names.add(version.getFileName());
        }
        return names;
    }

    /** What a plain HTTP GET answered. */
    static final class Raw {
        int status;
        byte[] bytes;
    }

    /**
     * A GET made by the check itself, not by the SDK (a share link is used by someone who has no SDK): refused
     * unless its host is 127.0.0.1, recorded in {@link #blocked} like the SDK's own requests. {@code authorization}
     * is sent as the Authorization header when not null.
     */
    static Raw rawGet(String url, String authorization) throws java.io.IOException {
        final String host = URI.create(url).getHost();
        if (!LOOPBACK_HOST.equals(host)) {
            blocked.add(String.valueOf(host));
            throw new Failure("blocked non-loopback host: " + host);
        }
        final java.net.HttpURLConnection connection = (java.net.HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(30_000);
            if (authorization != null) {
                connection.setRequestProperty("Authorization", authorization);
            }
            final Raw raw = new Raw();
            raw.status = connection.getResponseCode();
            try (java.io.InputStream in = raw.status < 400 ? connection.getInputStream() : connection.getErrorStream()) {
                raw.bytes = in == null ? new byte[0] : in.readAllBytes();
            }
            return raw;
        } finally {
            connection.disconnect();
        }
    }

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
