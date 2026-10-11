// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.B2ClientConfig;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2Headers;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.webApiHttpClient.B2StorageHttpClientBuilder;
import com.backblaze.b2.client.webApiHttpClient.HttpClientFactory;
import com.backblaze.b2.client.webApiHttpClient.HttpClientFactoryImpl;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Shared plumbing for the sdkharness conformance checks. Compiled together with one check class
 * by ../run-conformance (a JDK 11 single-file launch cannot import a second source file).
 *
 * <p>Each check is one {@code main} that calls {@link #run}. This prints exactly one result line,
 * {@code CONFORMANCE b2-sdk-java <capability> @simulator: PASS | FAIL (<step> -- <detail>)}, after
 * deleting every bucket and file the check created. Anything else it prints is a {@code NOTE} line.
 * It never prints a response body or a credential.
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

    private static String capability = "unknown";
    private static String step = "configuration";
    private static final List<B2Bucket> created = new ArrayList<>();

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
            // run-conformance checked this already; the SDK's http switch must never reach anything else.
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
        if (failure == null) {
            System.out.println("CONFORMANCE " + SLUG + " " + capability + " @simulator: PASS");
        } else {
            System.out.println("CONFORMANCE " + SLUG + " " + capability + " @simulator: FAIL (" + failure + ")");
            System.exit(1);
        }
    }

    private static B2StorageClient newClient(String masterUrl, boolean recordWire) {
        final B2ClientConfig config = B2ClientConfig.builder(KEY_ID, KEY, "b2-sdk-java-sdkharness-conformance")
                .setMasterUrl(masterUrl)
                .build();
        // The simulator speaks plain HTTP on loopback, so this enables the SDK's documented
        // test-environment switch; everything else is the default client.
        final HttpClientFactoryImpl.Builder httpBuilder = HttpClientFactoryImpl.builder().setSupportInsecureHttp(true);
        final HttpClientFactory factory = recordWire ? recordingFactory(httpBuilder.createRequestConfig())
                                                      : httpBuilder.build();
        return B2StorageHttpClientBuilder.builder(config).setHttpClientFactory(factory).build();
    }

    /** The SDK's own request settings, plus a record of each request's method and URI. */
    private static HttpClientFactory recordingFactory(RequestConfig requestConfig) {
        return new HttpClientFactory() {
            @Override
            public CloseableHttpClient create() {
                return HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .addInterceptorFirst((HttpRequestInterceptor) (request, context) ->
                                wire.add(request.getRequestLine().getMethod() + " " + request.getRequestLine().getUri()))
                        .build();
            }

            @Override
            public void close() {
            }
        };
    }

    /** Deletes every file version and bucket this check created, on the error path too. */
    private static void cleanup() {
        if (client == null) {
            return;
        }
        for (B2Bucket bucket : created) {
            try {
                client.deleteAllFilesInBucket(bucket.getBucketId());
                client.deleteBucket(bucket.getBucketId());
            } catch (B2Exception | RuntimeException e) {
                note("cleanup of " + bucket.getBucketName() + " failed: " + describe(e));
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

    static String newBucketName() {
        return "sdkharness-conf-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** Records a bucket the check created itself, so cleanup deletes it if the check does not. */
    static void track(B2Bucket bucket) {
        created.add(bucket);
    }

    /** The check deleted this bucket itself, so cleanup has nothing left to do for it. */
    static void untrack(B2Bucket bucket) {
        created.removeIf(b -> b.getBucketId().equals(bucket.getBucketId()));
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

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
