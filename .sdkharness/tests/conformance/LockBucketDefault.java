// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2BucketDefaultRetention;
import com.backblaze.b2.client.structures.B2FileRetention;
import com.backblaze.b2.client.structures.B2FileRetentionMode;
import com.backblaze.b2.client.structures.B2FileRetentionPeriodUnit;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;

/**
 * lock.bucket_default. Run by ../run-conformance (see ../../README.md).
 *
 * <p>Object Lock: a bucket's default retention means "All of the files that are uploaded or copied to that bucket,
 * from that time on, are protected by those Object Lock retention settings by default."
 * (https://www.backblaze.com/docs/cloud-storage-object-lock). The read-back shape, defaultRetention {mode, period
 * {duration, unit}}, is the native API's
 * (https://www.backblaze.com/docs/cloud-storage-enable-object-lock-with-the-native-api).
 *
 * <p>A file-lock-enabled bucket gets a default retention of governance, 1 day. Asserted: the update's response and
 * a fresh read of the bucket report the mode and the period, and a file uploaded afterwards inherits a governance
 * retention that ends about one day from now (within 30 minutes).
 */
public class LockBucketDefault {
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private static final long TOLERANCE_MILLIS = 30L * 60 * 1000;

    public static void main(String[] args) {
        Support.run("lock.bucket_default", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.lockedBucket();
            Support.check(bucket.getFileLockConfiguration().isFileLockEnabled(), "the new bucket does not report file lock as enabled");

            Support.step("set the default retention");
            final B2BucketDefaultRetention wanted = new B2BucketDefaultRetention(B2FileRetentionMode.GOVERNANCE, 1, B2FileRetentionPeriodUnit.DAYS);
            final B2Bucket updated = Support.client.updateBucket(B2UpdateBucketRequest.builder(bucket).setDefaultRetention(wanted).build());
            Support.eq("default retention in the update response", wanted, updated.getFileLockConfiguration().getDefaultRetention());

            Support.step("read back");
            final B2Bucket reread = Support.client.getBucketOrNullByName(bucket.getBucketName());
            Support.check(reread != null, "the bucket is not listed");
            Support.eq("default retention mode", B2FileRetentionMode.GOVERNANCE, reread.getFileLockConfiguration().getDefaultRetentionMode());
            Support.eq("default retention duration", 1, reread.getFileLockConfiguration().getDefaultRetentionPeriod().getDuration());
            Support.eq("default retention unit", B2FileRetentionPeriodUnit.DAYS, reread.getFileLockConfiguration().getDefaultRetentionPeriod().getUnit());
            Support.check(reread.getFileLockConfiguration().isFileLockEnabled(), "file lock is no longer enabled after the update");

            Support.step("an uploaded file inherits it");
            final long before = System.currentTimeMillis();
            final B2FileVersion uploaded = Support.upload(reread, "st/lock/default.txt", Support.payload(256, 81), "text/plain", null);
            final long after = System.currentTimeMillis();
            final B2FileRetention retention = Support.client.getFileInfo(uploaded.getFileId()).getFileRetention();
            Support.eq("inherited retention mode", B2FileRetentionMode.GOVERNANCE, retention.getMode());
            final long until = retention.getRetainUntilTimestamp();
            Support.check(until >= before + DAY_MILLIS - TOLERANCE_MILLIS && until <= after + DAY_MILLIS + TOLERANCE_MILLIS,
                    "inherited retention ends " + (until - before) / 60000 + " minutes from now, expected about " + DAY_MILLIS / 60000);
        });
    }
}
