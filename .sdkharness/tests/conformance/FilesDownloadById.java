// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;

import java.util.Arrays;

/**
 * files.download_by_id. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_download_file_by_id "downloads one file by providing the ID of the file", so with two versions
 * of one name each fileId returns its own bytes, while download by name returns the most recent
 * version.
 * https://www.backblaze.com/apidocs/b2-download-file-by-id
 * https://www.backblaze.com/apidocs/b2-download-file-by-name
 */
public class FilesDownloadById {
    public static void main(String[] args) {
        Support.run("files.download_by_id", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();

            Support.step("upload two versions");
            final String name = "st/by-id/versioned.bin";
            final byte[] first = Support.payload(2048, 3);
            final byte[] second = Support.payload(3000, 4);
            final B2FileVersion v1 = Support.upload(bucket, name, first, "application/octet-stream", null);
            final B2FileVersion v2 = Support.upload(bucket, name, second, "application/octet-stream", null);
            Support.check(!v1.getFileId().equals(v2.getFileId()), "two uploads of one name returned the same fileId");

            Support.step("download first version by id");
            final Support.Downloaded got1 = Support.downloadById(v1.getFileId());
            Support.check(Arrays.equals(first, got1.bytes), "the first fileId did not return the first version's bytes");
            Support.eq("first X-Bz-Content-Sha1", Support.sha1(first), got1.headers.getValueOrNull("X-Bz-Content-Sha1"));
            Support.eq("first X-Bz-File-Id", v1.getFileId(), got1.headers.getValueOrNull("X-Bz-File-Id"));

            Support.step("download second version by id");
            final Support.Downloaded got2 = Support.downloadById(v2.getFileId());
            Support.check(Arrays.equals(second, got2.bytes), "the second fileId did not return the second version's bytes");
            Support.eq("second X-Bz-File-Id", v2.getFileId(), got2.headers.getValueOrNull("X-Bz-File-Id"));

            Support.step("by name is the latest");
            final Support.Downloaded byName = Support.downloadByName(bucket.getBucketName(), name);
            Support.check(Arrays.equals(second, byName.bytes), "download by name did not return the most recent version");
        });
    }
}
