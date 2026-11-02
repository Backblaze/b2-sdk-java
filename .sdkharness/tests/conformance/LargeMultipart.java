// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

/**
 * large.multipart. Run by ../run-conformance (see ../../README.md).
 *
 * <p>A payload of three parts' worth of bytes (the part size is the larger of the realm's recommended and
 * absolute minimum part size, never a hard-coded 5 MB) is uploaded with {@code uploadLargeFile}. Asserted:
 * the SDK started one large file, sent more than one part and finished it; the file's contentSha1 is "none"
 * ("Large files do not have SHA1 checksums, and the value is none",
 * https://www.backblaze.com/apidocs/b2-finish-large-file) and the sha1 the caller gave travels as
 * {@code large_file_sha1} ("If the caller knows the SHA1 of the entire large file being uploaded, Backblaze
 * recommends using large_file_sha1 as the name", https://www.backblaze.com/apidocs/b2-start-large-file); the
 * downloaded bytes equal what was uploaded. A large file "must consist of at least two parts"
 * (https://www.backblaze.com/docs/cloud-storage-large-files).
 *
 * <p>NEEDS REVIEW (as in the Python check): the docs recommend large_file_sha1, they do not require it.
 */
public class LargeMultipart {
    public static void main(String[] args) {
        Support.run("large.multipart", true, () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();

            Support.step("upload");
            final long partSize = Support.partSize();
            final byte[] data = Support.payload((int) (3 * partSize + 17), 21);
            final String sha1 = Support.sha1(data);
            final ExecutorService executor = Support.executor(2);
            Support.wire.clear();
            final B2FileVersion large = Support.client.uploadLargeFile(
                    B2UploadFileRequest.builder(bucket.getBucketId(), "st/large/multipart.bin", "application/octet-stream",
                            B2ByteArrayContentSource.builder(data).setSha1OrNull(sha1).build()).build(),
                    executor);

            Support.step("the SDK used the large-file calls");
            Support.eq("b2_start_large_file requests", 1, Support.wireCount("POST", "b2_start_large_file"));
            Support.eq("b2_finish_large_file requests", 1, Support.wireCount("POST", "b2_finish_large_file"));
            final int parts = Support.wireCount("POST", "b2_upload_part");
            Support.check(parts > 1, "expected more than one b2_upload_part request, saw " + parts);
            Support.eq("b2_upload_file requests (a large file is not a small upload)", 0, Support.wireCount("POST", "b2_upload_file"));

            Support.step("the finished file");
            Support.eq("uploaded contentLength", (long) data.length, large.getContentLength());
            final B2FileVersion stored = Support.client.getFileInfo(large.getFileId());
            Support.eq("contentSha1 of a large file", "none", stored.getContentSha1());
            Support.eq("large_file_sha1", sha1, stored.getFileInfo().get("large_file_sha1"));
            Support.eq("stored contentLength", (long) data.length, stored.getContentLength());

            Support.step("bytes");
            Support.check(Arrays.equals(data, Support.downloadById(large.getFileId()).bytes),
                    "the downloaded bytes differ from the uploaded ones");
        });
    }
}
