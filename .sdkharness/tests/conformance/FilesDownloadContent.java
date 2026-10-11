// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2DownloadByNameRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.util.B2ByteRange;

import java.util.Arrays;

/**
 * files.download_content. Run by ../run-conformance (see ../../README.md).
 *
 * <p>Download by name returns the stored bytes with the headers B2 documents (Content-Length,
 * Content-Type, X-Bz-File-Id, X-Bz-File-Name, X-Bz-Content-Sha1); a Range request returns just that
 * byte range; a name that does not exist is a 404 not_found.
 * https://www.backblaze.com/apidocs/b2-download-file-by-name
 */
public class FilesDownloadContent {
    public static void main(String[] args) {
        Support.run("files.download_content", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();

            Support.step("upload");
            final String name = "st/download/content.bin";
            final byte[] data = Support.payload(4096, 2);
            final B2FileVersion uploaded = Support.upload(bucket, name, data, "application/x-sdkharness", null);

            Support.step("download by name");
            final Support.Downloaded got = Support.downloadByName(bucket.getBucketName(), name);
            Support.check(Arrays.equals(data, got.bytes), "downloaded bytes differ from the uploaded bytes");
            Support.eq("X-Bz-Content-Sha1", Support.sha1(data), got.headers.getValueOrNull("X-Bz-Content-Sha1"));
            Support.eq("Content-Length", String.valueOf(data.length), got.headers.getValueOrNull("Content-Length"));
            Support.eq("Content-Type", "application/x-sdkharness", got.headers.getContentType());
            Support.eq("X-Bz-File-Id", uploaded.getFileId(), got.headers.getValueOrNull("X-Bz-File-Id"));

            Support.step("range");
            final int first = 100;
            final int last = 199;
            final Support.Downloaded part = new Support.Downloaded();
            Support.client.downloadByName(
                    B2DownloadByNameRequest.builder(bucket.getBucketName(), name).setRange(B2ByteRange.between(first, last)).build(),
                    (headers, in) -> {
                        part.headers = headers;
                        part.bytes = in.readAllBytes();
                    });
            Support.check(Arrays.equals(Arrays.copyOfRange(data, first, last + 1), part.bytes),
                    "a Range of bytes " + first + "-" + last + " did not return exactly those bytes (got " + part.bytes.length + " bytes)");

            Support.step("missing name");
            final B2Exception missing = Support.expectError("download of a missing name",
                    () -> Support.downloadByName(bucket.getBucketName(), "st/download/no-such-file"));
            Support.eq("missing name status", 404, missing.getStatus());
        });
    }
}
