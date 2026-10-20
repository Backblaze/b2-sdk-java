// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2DownloadAuthorization;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2GetDownloadAuthorizationRequest;

import java.util.Arrays;

/**
 * urls.native_download. Run by ../run-conformance (see ../../README.md).
 *
 * <p>The share link is B2's native one, never an S3 presigned URL. b2_get_download_authorization "Returns an
 * authorization token that can be passed to ******* in the Authorization header or as an
 * Authorization parameter", limited to a fileNamePrefix and valid for validDurationInSeconds
 * (https://www.backblaze.com/apidocs/b2-get-download-authorization). ******* addresses
 * {@code <downloadUrl>/file/<bucket>/<name>}, answers a private file without a token with 401, and an expired token
 * with 401 expired_auth_token (https://www.backblaze.com/apidocs/b2-download-file-by-name).
 *
 * <p>Asserted, over plain HTTP made by the check itself (the holder of a share link has no SDK): the SDK's
 * {@code getDownloadByNameUrl} is a /file/<bucket>/<name> URL with no X-Amz-* material; the file is 401 without a
 * token; with the token (Authorization header) it is 200 and the bytes are right; a file outside the token's
 * prefix is 401; after the validity has passed the same token is 401.
 */
public class UrlsNativeDownload {
    private static final int VALID_SECONDS = 2;

    public static void main(String[] args) {
        Support.run("urls.native_download", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();

            Support.step("files");
            final byte[] data = Support.payload(300, 61);
            final B2FileVersion shared = Support.upload(bucket, "st/share/file.bin", data, "application/octet-stream", null);
            Support.upload(bucket, "st/other/file.bin", data, "application/octet-stream", null);

            Support.step("native url");
            final String url = Support.client.getDownloadByNameUrl(bucket.getBucketName(), shared.getFileName());
            Support.check(url.contains("/file/" + bucket.getBucketName() + "/st/share/file.bin"), "not a native /file/<bucket>/<name> URL: " + url.replaceAll("\\?.*", "?..."));
            Support.check(!url.contains("X-Amz-"), "the URL carries S3 presigning material");

            Support.step("no token");
            Support.eq("a private file without a token", 401, Support.rawGet(url, null).status);

            Support.step("authorization");
            final B2DownloadAuthorization authorization = Support.client.getDownloadAuthorization(
                    B2GetDownloadAuthorizationRequest.builder(bucket.getBucketId(), "st/share/", VALID_SECONDS).build());
            Support.eq("authorization prefix", "st/share/", authorization.getFileNamePrefix());
            Support.eq("authorization bucket", bucket.getBucketId(), authorization.getBucketId());
            final long issued = System.nanoTime();

            Support.step("token works within its validity");
            final Support.Raw ok = Support.rawGet(url, authorization.getAuthorizationToken());
            Support.eq("status with the token", 200, ok.status);
            Support.check(Arrays.equals(data, ok.bytes), "the bytes fetched with the token differ from the upload");

            Support.step("token is limited to its prefix");
            final String otherUrl = Support.client.getDownloadByNameUrl(bucket.getBucketName(), "st/other/file.bin");
            Support.eq("a file outside the token's prefix", 401, Support.rawGet(otherUrl, authorization.getAuthorizationToken()).status);

            Support.step("token expires");
            final long wait = VALID_SECONDS * 1000L + 1500 - (System.nanoTime() - issued) / 1_000_000;
            if (wait > 0) {
                Thread.sleep(wait);
            }
            Support.eq("status after the validity has passed", 401, Support.rawGet(url, authorization.getAuthorizationToken()).status);
        });
    }
}
