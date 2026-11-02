// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2LifecycleRule;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * bucket.lifecycle. Run by ../run-conformance (see ../../README.md).
 *
 * <p>daysFromHidingToDeleting and fileNamePrefix are documented rule fields; B2's own example rule is
 * daysFromHidingToDeleting 1 with daysFromUploadingToHiding null. "Unspecified properties are treated as having a
 * null value", which is the expected value of every field not set. b2_update_bucket lifecycleRules: "If specified,
 * the existing lifecycle rules will be replaced with this new list."
 * (https://www.backblaze.com/docs/cloud-storage-lifecycle-rules, https://www.backblaze.com/apidocs/b2-update-bucket).
 *
 * <p>Asserted: a rule {prefix st/, daysFromHidingToDeleting 1} set on an existing bucket is read back with the same
 * values and no other field populated; setting a different list replaces the first (the read-back is exactly the new
 * list, all three day fields included); an empty list removes every rule.
 */
public class BucketLifecycle {
    public static void main(String[] args) {
        Support.run("bucket.lifecycle", () -> {
            Support.step("bucket");
            final B2Bucket bucket = Support.bucket();
            Support.eq("rules on a new bucket", 0, bucket.getLifecycleRules().size());

            Support.step("set one rule");
            final B2LifecycleRule rule = B2LifecycleRule.builder("st/").setDaysFromHidingToDeleting(1).build();
            final B2Bucket withRule = Support.client.updateBucket(B2UpdateBucketRequest.builder(bucket).setLifecycleRules(Collections.singletonList(rule)).build());
            final List<B2LifecycleRule> read = listed(bucket).getLifecycleRules();
            Support.eq("number of rules", 1, read.size());
            Support.eq("fileNamePrefix", "st/", read.get(0).getFileNamePrefix());
            Support.eq("daysFromHidingToDeleting", 1, read.get(0).getDaysFromHidingToDeleting());
            Support.eq("daysFromUploadingToHiding (not set)", null, read.get(0).getDaysFromUploadingToHiding());
            Support.eq("daysFromStartingToCancelingUnfinishedLargeFiles (not set)", null, read.get(0).getDaysFromStartingToCancelingUnfinishedLargeFiles());

            Support.step("replace the rules");
            final B2LifecycleRule hide = B2LifecycleRule.builder("logs/").setDaysFromUploadingToHiding(30).setDaysFromHidingToDeleting(7).build();
            final B2LifecycleRule cancel = B2LifecycleRule.builder("big/").setDaysFromStartingToCancelingUnfinishedLargeFiles(2).build();
            // updateBucket is optimistic: each update names the revision it was made from, so it starts from the last response.
            final B2Bucket replacedBucket = Support.client.updateBucket(B2UpdateBucketRequest.builder(withRule).setLifecycleRules(Arrays.asList(hide, cancel)).build());
            final List<B2LifecycleRule> replaced = listed(bucket).getLifecycleRules();
            Support.eq("number of rules after the replacement", 2, replaced.size());
            for (B2LifecycleRule expected : Arrays.asList(hide, cancel)) {
                B2LifecycleRule actual = null;
                for (B2LifecycleRule candidate : replaced) {
                    if (expected.getFileNamePrefix().equals(candidate.getFileNamePrefix())) {
                        actual = candidate;
                    }
                }
                Support.check(actual != null, "no rule for prefix " + expected.getFileNamePrefix() + " after the replacement");
                Support.eq(expected.getFileNamePrefix() + " daysFromUploadingToHiding", expected.getDaysFromUploadingToHiding(), actual.getDaysFromUploadingToHiding());
                Support.eq(expected.getFileNamePrefix() + " daysFromHidingToDeleting", expected.getDaysFromHidingToDeleting(), actual.getDaysFromHidingToDeleting());
                Support.eq(expected.getFileNamePrefix() + " daysFromStartingToCancelingUnfinishedLargeFiles",
                        expected.getDaysFromStartingToCancelingUnfinishedLargeFiles(), actual.getDaysFromStartingToCancelingUnfinishedLargeFiles());
            }

            Support.step("remove the rules");
            Support.client.updateBucket(B2UpdateBucketRequest.builder(replacedBucket).setLifecycleRules(Collections.emptyList()).build());
            Support.eq("rules after clearing", 0, listed(bucket).getLifecycleRules().size());
        });
    }

    private static B2Bucket listed(B2Bucket bucket) throws Exception {
        final B2Bucket found = Support.client.getBucketOrNullByName(bucket.getBucketName());
        Support.check(found != null, "the bucket is not listed");
        return found;
    }
}
