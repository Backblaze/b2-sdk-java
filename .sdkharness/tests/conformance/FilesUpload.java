// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2ListFileNamesRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * files.upload. Run by ../run-conformance (see ../../README.md).
 *
 * <p>Upload, then check what B2 documents about the stored file: b2_upload_file returns the file's
 * name, size, contentSha1 (the SHA1 of the bytes stored), content type, fileId and fileInfo, with
 * custom keys lower-cased, and the bytes read back are the bytes sent.
 * https://www.backblaze.com/apidocs/b2-upload-file
 */
public class FilesUpload {
    public static void main(String[] args) {
        Support.run("files.upload", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();

            Support.step("upload");
            final String name = "st/upload/data.bin";
            final byte[] data = Support.payload(64 * 1024 + 17, 1);
            final Map<String, String> info = Collections.singletonMap("sdkharness-key", "sdkharness-value");
            final B2FileVersion uploaded = Support.upload(bucket, name, data, "application/x-sdkharness", info);

            Support.step("response");
            Support.eq("action", "upload", uploaded.getAction());
            Support.eq("fileName", name, uploaded.getFileName());
            Support.eq("contentLength", (long) data.length, uploaded.getContentLength());
            Support.eq("contentSha1", Support.sha1(data), uploaded.getContentSha1());
            Support.eq("contentType", "application/x-sdkharness", uploaded.getContentType());
            Support.eq("fileInfo", "sdkharness-value", uploaded.getFileInfo().get("sdkharness-key"));
            Support.check(uploaded.getFileId() != null && !uploaded.getFileId().isEmpty(), "fileId is empty");

            Support.step("list");
            boolean listed = false;
            for (B2FileVersion v : Support.client.fileNames(B2ListFileNamesRequest.builder(bucket.getBucketId()).setPrefix(name).build())) {
                listed |= name.equals(v.getFileName()) && uploaded.getFileId().equals(v.getFileId());
            }
            Support.check(listed, "the uploaded file is not listed under its fileId");

            Support.step("download");
            final Support.Downloaded got = Support.downloadById(uploaded.getFileId());
            Support.check(Arrays.equals(data, got.bytes), "downloaded bytes differ from the uploaded bytes");
            Support.eq("download X-Bz-Content-Sha1", Support.sha1(data), got.headers.getValueOrNull("X-Bz-Content-Sha1"));

            Support.step("empty file");
            final B2FileVersion empty = Support.upload(bucket, "st/upload/empty.bin", new byte[0], "application/octet-stream", null);
            Support.eq("empty contentLength", 0L, empty.getContentLength());
            Support.eq("empty contentSha1", "da39a3ee5e6b4b0d3255bfef95601890afd80709", empty.getContentSha1());
            Support.eq("empty download length", 0, Support.downloadById(empty.getFileId()).bytes.length);

            Support.step("unusual name");
            final String odd = "st/upload/sp ace/ünï+cöde.txt";
            final byte[] oddData = Support.utf8("unusual name");
            final B2FileVersion oddVersion = Support.upload(bucket, odd, oddData, "text/plain", null);
            Support.eq("unusual fileName", odd, oddVersion.getFileName());
            final Support.Downloaded oddGot = Support.downloadByName(bucket.getBucketName(), odd);
            Support.check(Arrays.equals(oddData, oddGot.bytes), "bytes of the unusually named file differ");
        });
    }
}
