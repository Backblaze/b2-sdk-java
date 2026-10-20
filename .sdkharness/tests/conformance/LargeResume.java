// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.exceptions.B2LocalException;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2Part;
import com.backblaze.b2.client.structures.B2ListUnfinishedLargeFilesRequest;
import com.backblaze.b2.client.structures.B2UploadFileRequest;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * large.resume. Run by ../run-conformance (see ../../README.md).
 *
 * <p>"Any number of large files can be in progress at the same time. You can use b2_list_unfinished_large_files
 * to get a list of them. For any one unfinished large file, you can use b2_list_parts to get a list of the parts
 * that were uploaded so far." (https://www.backblaze.com/docs/cloud-storage-create-large-files-with-the-native-api;
 * https://www.backblaze.com/apidocs/b2-list-parts). B2 makes reusing the unfinished fileId possible; that the SDK
 * does it is the capability under test, not a B2 promise.
 *
 * <p>The first upload is interrupted from inside the content source: the bytes of the third part cannot be read
 * (an unchecked exception, not an IOException: the SDK retries an IOException for about two minutes), so with one
 * worker the first two parts are stored and the call fails before the file is finished. The large file is then
 * unfinished (a "start" version in the unfinished listing, parts in b2_list_parts). The SDK's resume call
 * {@code finishUploadingLargeFile} is given that file and the same content. Asserted: the rerun sends no
 * b2_start_large_file, uploads only the parts that were missing (fewer than the total, and exactly the missing
 * ones), finishes the same fileId, and the stored bytes equal the content.
 *
 * <p>Two legs, because the SDK treats the two content shapes differently. Leg "no sha1": the content source does
 * not know its sha1, so the large file carries no large_file_sha1 and the rerun is the same request as the first
 * run. Leg "sha1": the content source knows its sha1 (the SDK records it as the large_file_sha1 file info); the
 * rerun is again the same request as the first run. If the SDK refuses it with a "mismatch" error, the scenario
 * FAILS with a FINDING as the reason: the check detects, it does not work around the refusal. The finding is about
 * the SDK, not about B2: B2LargeFileUploader removes the key
 * {@code X-Bz-Info-large_file_sha1} (B2Headers.LARGE_FILE_SHA1) from the file info before comparing it with the
 * request's, but the info map is keyed {@code large_file_sha1} (B2Headers.LARGE_FILE_SHA1_INFO_NAME).
 */
public class LargeResume {
    private static final int PARTS = 4;

    public static void main(String[] args) {
        Support.run("large.resume", true, () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();
            final long partSize = Support.partSize();
            final byte[] data = Support.payload((int) (PARTS * partSize + 3), 41);

            Support.step("leg no sha1");
            resume(bucket, "st/large/resume-no-sha1.bin", data, null);

            Support.step("leg sha1");
            resume(bucket, "st/large/resume-sha1.bin", data, Support.sha1(data));
        });
    }

    private static void resume(B2Bucket bucket, String name, byte[] data, String sha1) throws Exception {
        final long partSize = Support.partSize();
        final String leg = sha1 == null ? "no sha1: " : "sha1: ";

        Support.wire.clear();
        try {
            Support.client.uploadLargeFile(request(bucket, name, new Source(data, sha1, 2 * partSize)), Support.executor(1));
            Support.fail(leg + "the interrupted upload finished instead of failing");
        } catch (B2Exception expected) {
            Support.note(leg + "the first upload stopped as intended: " + Support.describe(expected));
        }
        Support.eq(leg + "b2_start_large_file requests in the first run", 1, Support.wireCount("POST", "b2_start_large_file"));
        Support.eq(leg + "b2_finish_large_file requests in the first run", 0, Support.wireCount("POST", "b2_finish_large_file"));

        final List<B2FileVersion> unfinished = unfinished(bucket, name);
        Support.eq(leg + "unfinished large files named " + name, 1, unfinished.size());
        final B2FileVersion pending = unfinished.get(0);
        Support.eq(leg + "unfinished file name", name, pending.getFileName());
        int storedBefore = 0;
        for (B2Part part : Support.client.parts(pending.getFileId())) {
            storedBefore++;
        }
        Support.check(storedBefore >= 1, leg + "no part was stored before the interruption");
        Support.check(storedBefore < PARTS, leg + "all " + PARTS + " parts were stored; nothing is left to resume");

        Support.wire.clear();
        final B2FileVersion finished;
        try {
            finished = Support.client.finishUploadingLargeFile(pending, request(bucket, name, new Source(data, sha1, -1)), Support.executor(2));
        } catch (B2LocalException e) {
            if (sha1 == null || !"mismatch".equals(e.getCode())) {
                throw e;
            }
            Support.fail("FINDING b2-sdk-java: finishUploadingLargeFile refused the same request that started the large file: "
                    + e.getMessage());
            return;
        }
        Support.eq(leg + "b2_start_large_file requests in the rerun", 0, Support.wireCount("POST", "b2_start_large_file"));
        Support.eq(leg + "finished fileId is the unfinished one", pending.getFileId(), finished.getFileId());
        Support.eq(leg + "parts uploaded in the rerun", PARTS - storedBefore, Support.wireCount("POST", "b2_upload_part"));
        Support.eq(leg + "b2_finish_large_file requests in the rerun", 1, Support.wireCount("POST", "b2_finish_large_file"));

        Support.eq(leg + "large files still unfinished after the rerun", 0, unfinished(bucket, name).size());
        Support.eq(leg + "stored contentLength", (long) data.length, Support.client.getFileInfo(finished.getFileId()).getContentLength());
        Support.check(Arrays.equals(data, Support.downloadById(finished.getFileId()).bytes), leg + "the downloaded bytes differ from the content");
    }

    private static List<B2FileVersion> unfinished(B2Bucket bucket, String name) throws B2Exception {
        final List<B2FileVersion> all = new ArrayList<>();
        for (B2FileVersion v : Support.client.unfinishedLargeFiles(
                B2ListUnfinishedLargeFilesRequest.builder(bucket.getBucketId()).setNamePrefix(name).build())) {
            all.add(v);
        }
        return all;
    }

    private static B2UploadFileRequest request(B2Bucket bucket, String name, B2ContentSource source) {
        return B2UploadFileRequest.builder(bucket.getBucketId(), name, "application/octet-stream", source).build();
    }

    /** The content; with {@code failAt >= 0}, reading past that byte offset fails the way an interrupted run would. */
    private static final class Source implements B2ContentSource {
        private final byte[] data;
        private final String sha1;
        private final long failAt;

        Source(byte[] data, String sha1, long failAt) {
            this.data = data;
            this.sha1 = sha1;
            this.failAt = failAt;
        }

        @Override
        public long getContentLength() {
            return data.length;
        }

        @Override
        public String getSha1OrNull() {
            return sha1;
        }

        @Override
        public Long getSrcLastModifiedMillisOrNull() {
            return null;
        }

        @Override
        public InputStream createInputStream() {
            return new FilterInputStream(new ByteArrayInputStream(data)) {
                private long pos;

                private void guard() {
                    if (failAt >= 0 && pos >= failAt) {
                        throw new IllegalStateException("interrupted by the check at byte " + pos);
                    }
                }

                @Override
                public int read() throws IOException {
                    guard();
                    final int b = super.read();
                    if (b >= 0) {
                        pos++;
                    }
                    return b;
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    guard();
                    final int n = super.read(b, off, failAt >= 0 ? (int) Math.min(len, failAt - pos) : len);
                    if (n > 0) {
                        pos += n;
                    }
                    return n;
                }

                @Override
                public long skip(long n) throws IOException {
                    final long skipped = super.skip(n);
                    pos += skipped;
                    return skipped;
                }
            };
        }
    }
}
