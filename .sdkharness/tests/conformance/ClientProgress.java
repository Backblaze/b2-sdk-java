// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.contentSources.B2ByteArrayContentSource;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.structures.B2UploadListener;
import com.backblaze.b2.client.structures.B2UploadProgress;
import com.backblaze.b2.client.structures.B2UploadState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * client.progress. Run by ../run-conformance (see ../../README.md).
 *
 * <p>NO DOCUMENTED CONTRACT: progress reporting is the SDK's own ergonomics (B2UploadListener); the B2 API has no
 * progress surface, so the expected values come from the scenario, not from a Backblaze page. Scenario: a large
 * object is uploaded with a listener attached; the callbacks fire, are monotonically non-decreasing, and the final
 * value equals the total byte count.
 *
 * <p>One worker, deliberately: with several, the per-part reports of concurrent parts legitimately interleave, and
 * "non-decreasing" would test the thread pool, not the listener. The SDK reports per part (part index, bytes so far,
 * state), so the check sums the latest report of every part into one running total. Asserted: callbacks fire; each
 * part's own bytes-so-far never goes down; the running total never goes down; every part ends SUCCEEDED with
 * bytes-so-far equal to its length; the final total equals the byte count of the object and the part lengths add up
 * to it.
 */
public class ClientProgress {
    public static void main(String[] args) {
        Support.run("client.progress", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();
            final byte[] data = Support.payload((int) (4 * Support.partSize() + 5), 71);

            Support.step("upload with a listener");
            final List<B2UploadProgress> reports = Collections.synchronizedList(new ArrayList<>());
            final B2UploadListener listener = reports::add;
            final B2FileVersion file = Support.client.uploadLargeFile(
                    B2UploadFileRequest.builder(bucket.getBucketId(), "st/progress/large.bin", "application/octet-stream",
                            B2ByteArrayContentSource.build(data)).setListener(listener).build(),
                    Support.executor(1));
            Support.eq("uploaded contentLength", (long) data.length, file.getContentLength());

            Support.step("callbacks fired");
            Support.check(!reports.isEmpty(), "the listener was never called");

            Support.step("monotonic");
            final Map<Integer, Long> latest = new TreeMap<>();
            final Map<Integer, Long> lengths = new TreeMap<>();
            long previousTotal = 0;
            synchronized (reports) {
                for (B2UploadProgress report : reports) {
                    final Long before = latest.get(report.getPartIndex());
                    Support.check(before == null || report.getBytesSoFar() >= before,
                            "part " + report.getPartIndex() + " went backwards from " + before + " to " + report.getBytesSoFar() + " (" + report.getState() + ")");
                    latest.put(report.getPartIndex(), report.getBytesSoFar());
                    lengths.put(report.getPartIndex(), report.getLength());
                    long total = 0;
                    for (long bytes : latest.values()) {
                        total += bytes;
                    }
                    Support.check(total >= previousTotal, "the running total went backwards from " + previousTotal + " to " + total);
                    previousTotal = total;
                }
            }

            Support.step("final value");
            Support.check(latest.size() > 1, "expected progress for more than one part, saw " + latest.size());
            Support.eq("final running total", (long) data.length, previousTotal);
            long lengthSum = 0;
            for (long length : lengths.values()) {
                lengthSum += length;
            }
            Support.eq("sum of the parts' lengths", (long) data.length, lengthSum);
            for (int part : latest.keySet()) {
                B2UploadProgress last = null;
                synchronized (reports) {
                    for (B2UploadProgress report : reports) {
                        if (report.getPartIndex() == part) {
                            last = report;
                        }
                    }
                }
                Support.eq("part " + part + " final state", B2UploadState.SUCCEEDED, last.getState());
                Support.eq("part " + part + " final bytes", last.getLength(), last.getBytesSoFar());
            }
        });
    }
}
