// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.B2ClientConfig;
import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;
import com.backblaze.b2.client.structures.B2ListFileVersionsRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.webApiHttpClient.B2StorageHttpClientBuilder;
import com.backblaze.b2.client.webApiHttpClient.HttpClientFactoryImpl;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * Customer golden path against the sdkharness simulator: authorize, upload, byte-verified
 * download, list, delete, and confirm the file is gone. Run by ../run-health, which checks
 * the simulator URL and supplies the classpath. Prints exactly one verdict line:
 * {@code GOLDEN-PATH PASS} or {@code GOLDEN-PATH FAIL <step>: <detail>}.
 */
public class GoldenPath {
    // The simulator's fixed test credential. No other credential is ever used.
    private static final String KEY_ID = "test-key-id";
    private static final String KEY = "test-key";

    private static String step = "configuration";

    public static void main(String[] args) {
        final String masterUrl = System.getenv("SDKHARNESS_SIMULATOR_URL");
        final String bucketName = System.getenv("B2_BUCKET_NAME");
        if (masterUrl == null || bucketName == null || bucketName.isEmpty()) {
            fail("simulator URL or bucket name is missing");
        }
        final B2ClientConfig config = B2ClientConfig.builder(KEY_ID, KEY, "b2-sdk-java-sdkharness-health")
                .setMasterUrl(masterUrl)
                .build();
        final String name = "sdkharness-health-check/" + UUID.randomUUID() + ".txt";
        final byte[] payload = ("b2-sdk-java health check " + name).getBytes(StandardCharsets.UTF_8);

        // The simulator speaks plain HTTP on loopback (run-health refuses any other URL), so this
        // enables the SDK's documented test-environment switch; everything else is the default client.
        try (B2StorageClient client = B2StorageHttpClientBuilder.builder(config)
                .setHttpClientFactory(HttpClientFactoryImpl.builder().setSupportInsecureHttp(true).build())
                .build()) {
            step = "authorize";
            client.getAccountAuthorization();

            step = "bucket";
            final B2Bucket bucket = client.getBucketOrNullByName(bucketName);
            if (bucket == null) {
                fail("bucket " + bucketName + " not found");
            }
            final String bucketId = bucket.getBucketId();

            step = "upload";
            final B2FileVersion uploaded = client.uploadSmallFile(B2UploadFileRequest
                    .builder(bucketId, name, B2ContentTypes.TEXT_PLAIN, B2ByteArrayContentSource.build(payload))
                    .build());

            step = "download";
            final B2ContentMemoryWriter sink = B2ContentMemoryWriter.build();
            client.downloadById(uploaded.getFileId(), sink);
            if (!Arrays.equals(sink.getBytes(), payload)) {
                fail("downloaded bytes differ from the uploaded bytes");
            }

            step = "list";
            boolean listed = false;
            for (B2FileVersion version : client.fileNames(B2ListFileNamesRequest.builder(bucketId).setPrefix(name).build())) {
                listed |= name.equals(version.getFileName()) && uploaded.getFileId().equals(version.getFileId());
            }
            if (!listed) {
                fail("the uploaded file is not listed");
            }

            step = "delete";
            client.deleteFileVersion(uploaded);

            step = "absence";
            for (B2FileVersion version : client.fileVersions(B2ListFileVersionsRequest.builder(bucketId).setPrefix(name).build())) {
                if (name.equals(version.getFileName())) {
                    fail("a version is still listed after delete");
                }
            }
        } catch (Exception e) {
            fail(e.getClass().getSimpleName() + " " + e.getMessage());
        }
        System.out.println("GOLDEN-PATH PASS");
    }

    private static void fail(String detail) {
        System.out.println("GOLDEN-PATH FAIL " + step + ": " + String.valueOf(detail).replaceAll("\\s+", " "));
        System.exit(1);
    }
}
