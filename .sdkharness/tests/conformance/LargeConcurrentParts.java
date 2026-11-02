// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

/**
 * large.concurrent_parts. Run by ../run-conformance (see ../../README.md).
 *
 * <p>B2 permits uploading the parts of one large file in parallel: "Parts of a large file can be uploaded and
 * copied in parallel" (https://www.backblaze.com/docs/cloud-storage-large-files) and "Each thread that uploads
 * should get its own URL" (https://www.backblaze.com/docs/cloud-storage-create-large-files-with-the-native-api).
 * No B2 document requires a client to do it, so the expected value comes from the SDK's own contract: the
 * executor passed to {@code uploadLargeFile} sets how many parts are sent at once.
 *
 * <p>KNOWN WEAK by design, and stated here rather than hidden: the check does not trust wall time. The same
 * eight-part file is uploaded twice, once on a one-thread executor and once on a four-thread executor, and the
 * HTTP client the check hands the SDK counts how many {@code b2_upload_part} requests are in flight at the same
 * instant (from the SDK starting to send to the response arriving). Each part's body is read slowly, so
 * simultaneous requests overlap on a loopback server. Asserted: with one worker never more than one is in
 * flight (this proves the instrument can tell the difference), with four workers more than one is. It is an
 * in-process observation at the SDK's own HTTP client, not a packet capture; the wall times are only a NOTE.
 */
public class LargeConcurrentParts {
    private static final int PARTS = 8;
    private static final long PAUSE_MILLIS = 150;

    public static void main(String[] args) {
        Support.run("large.concurrent_parts", true, () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();
            final long partSize = Support.partSize();
            final byte[] data = Support.payload((int) (PARTS * partSize + 3), 31);
            final String sha1 = Support.sha1(data);
            Support.inFlightMarker = "b2_upload_part";

            Support.step("one worker");
            final long oneMillis = upload(bucket, "st/large/serial.bin", data, sha1, Support.executor(1));
            final int serialMax = Support.maxInFlight.get();
            Support.eq("b2_upload_part requests in flight at once with one worker", 1, serialMax);

            Support.step("four workers");
            Support.maxInFlight.set(0);
            Support.inFlight.set(0);
            final long fourMillis = upload(bucket, "st/large/parallel.bin", data, sha1, Support.executor(4));
            final int parallelMax = Support.maxInFlight.get();
            Support.check(parallelMax > 1, "with four workers at most " + parallelMax + " b2_upload_part request(s) were ever in flight at once");

            Support.step("bytes");
            for (String name : new String[] {"st/large/serial.bin", "st/large/parallel.bin"}) {
                final B2FileVersion stored = Support.versions(bucket.getBucketId(), name).get(0);
                Support.check(Arrays.equals(data, Support.downloadById(stored.getFileId()).bytes), name + ": the downloaded bytes differ");
            }
            Support.note("evidence is in-process: peak b2_upload_part requests in flight " + serialMax + " with 1 worker and " + parallelMax
                    + " with 4; wall time " + oneMillis + " ms and " + fourMillis + " ms (a corroboration, never the verdict)");
        });
    }

    private static long upload(B2Bucket bucket, String name, byte[] data, String sha1, ExecutorService executor) throws Exception {
        final long start = System.nanoTime();
        final B2FileVersion file = Support.client.uploadLargeFile(
                B2UploadFileRequest.builder(bucket.getBucketId(), name, "application/octet-stream", new SlowSource(data, sha1)).build(),
                executor);
        Support.eq(name + " contentLength", (long) data.length, file.getContentLength());
        return (System.nanoTime() - start) / 1_000_000;
    }

    /** The bytes, read slowly: the first read of every stream waits, so uploads that run together overlap. */
    private static final class SlowSource implements B2ContentSource {
        private final byte[] data;
        private final String sha1;

        SlowSource(byte[] data, String sha1) {
            this.data = data;
            this.sha1 = sha1;
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
                private boolean first = true;

                private void pause() throws IOException {
                    if (first) {
                        first = false;
                        try {
                            Thread.sleep(PAUSE_MILLIS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("interrupted");
                        }
                    }
                }

                @Override
                public int read() throws IOException {
                    pause();
                    return super.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    pause();
                    return super.read(b, off, len);
                }
            };
        }
    }
}
