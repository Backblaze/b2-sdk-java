/*
 * Copyright 2026, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client.webApiHttpClient;

import com.backblaze.b2.client.B2ClientConfig;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2BucketTypes;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Round trip against a live B2 API: authorize, create bucket, upload, download and compare,
 * list, delete.
 *
 * <p>Configuration comes from the environment. Every test is skipped (JUnit assumption,
 * not a failure) unless all three are set:
 * <ul>
 *   <li>{@code B2_ENDPOINT}: the master URL (e.g. {@code https://api.backblazeb2.com}, or a
 *       local B2-compatible server such as {@code http://127.0.0.1:PORT});</li>
 *   <li>{@code B2_APPLICATION_KEY_ID}</li>
 *   <li>{@code B2_APPLICATION_KEY}</li>
 * </ul>
 * Plain http is accepted only for a loopback host; any other http endpoint fails the test.
 * Everything the test creates is removed in {@link #cleanUp()}.
 */
public class B2StorageClientIntegrationTest {
    private static final String ENDPOINT_VAR = "B2_ENDPOINT";
    private static final String KEY_ID_VAR = "B2_APPLICATION_KEY_ID";
    private static final String KEY_VAR = "B2_APPLICATION_KEY";

    private static B2StorageClient client;
    private static B2Bucket bucket;

    @BeforeClass
    public static void setUp() throws B2Exception {
        final String endpoint = nonEmptyEnv(ENDPOINT_VAR);
        final String keyId = nonEmptyEnv(KEY_ID_VAR);
        final String key = nonEmptyEnv(KEY_VAR);
        if (endpoint == null || keyId == null || key == null) {
            return; // not configured: every test is skipped by requireConfigured()
        }

        final boolean loopbackHttp = isLoopbackHttp(endpoint);
        assertTrue(ENDPOINT_VAR + " must be https, or http on a loopback host: " + endpoint,
                loopbackHttp || endpoint.startsWith("https://"));

        final B2ClientConfig config = B2ClientConfig
                .builder(keyId, key, "b2-sdk-java-integration-test")
                .setMasterUrl(endpoint)
                .build();
        final B2StorageHttpClientBuilder builder = B2StorageHttpClientBuilder.builder(config);
        if (loopbackHttp) {
            // The documented test-environment switch; only ever set for loopback.
            builder.setHttpClientFactory(HttpClientFactoryImpl.builder().setSupportInsecureHttp(true).build());
        }
        client = builder.build();

        // authorize (implicit on first call) and create a uniquely named bucket
        client.getAccountAuthorization();
        final String bucketName = "b2sdk-it-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        bucket = client.createBucket(bucketName, B2BucketTypes.ALL_PRIVATE);
    }

    /** Skipped (not failed) when the environment is not configured; reported per test. */
    @Before
    public void requireConfigured() {
        Assume.assumeNotNull(client);
        Assume.assumeNotNull(bucket);
    }

    @AfterClass
    public static void cleanUp() throws B2Exception {
        if (client == null) {
            return;
        }
        try {
            if (bucket != null) {
                final List<B2FileVersion> leftovers = new ArrayList<B2FileVersion>();
                for (B2FileVersion v : client.fileVersions(B2ListFileVersionsRequest.builder(bucket.getBucketId()).build())) {
                    leftovers.add(v);
                }
                for (B2FileVersion v : leftovers) {
                    client.deleteFileVersion(v);
                }
                client.deleteBucket(bucket.getBucketId());
            }
        } finally {
            client.close();
        }
    }

    @Test
    public void testUploadDownloadListDelete() throws B2Exception {
        assertNotNull("bucket was created", client.getBucketOrNullByName(bucket.getBucketName()));

        final String name = "integration/" + UUID.randomUUID() + ".txt";
        final byte[] payload = ("b2-sdk-java integration test " + name).getBytes(StandardCharsets.UTF_8);

        final B2FileVersion uploaded = client.uploadSmallFile(B2UploadFileRequest
                .builder(bucket.getBucketId(), name, B2ContentTypes.TEXT_PLAIN, B2ByteArrayContentSource.build(payload))
                .build());
        assertEquals(name, uploaded.getFileName());
        assertEquals(payload.length, uploaded.getContentLength());

        final B2ContentMemoryWriter byId = B2ContentMemoryWriter.build();
        client.downloadById(uploaded.getFileId(), byId);
        assertArrayEquals("download by id", payload, byId.getBytes());

        final B2ContentMemoryWriter byName = B2ContentMemoryWriter.build();
        client.downloadByName(bucket.getBucketName(), name, byName);
        assertArrayEquals("download by name", payload, byName.getBytes());

        boolean listed = false;
        for (B2FileVersion v : client.fileNames(B2ListFileNamesRequest.builder(bucket.getBucketId()).setPrefix(name).build())) {
            listed |= name.equals(v.getFileName()) && uploaded.getFileId().equals(v.getFileId());
        }
        assertTrue("uploaded file is listed", listed);

        client.deleteFileVersion(uploaded);

        for (B2FileVersion v : client.fileVersions(B2ListFileVersionsRequest.builder(bucket.getBucketId()).setPrefix(name).build())) {
            assertTrue("no version of " + name + " remains after delete", !name.equals(v.getFileName()));
        }
    }

    private static String nonEmptyEnv(String name) {
        final String v = System.getenv(name);
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }

    private static boolean isLoopbackHttp(String endpoint) {
        return endpoint.startsWith("http://127.0.0.1")
                || endpoint.startsWith("http://localhost")
                || endpoint.startsWith("http://[::1]");
    }
}
