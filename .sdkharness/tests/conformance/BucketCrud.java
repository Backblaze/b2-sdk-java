// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2CreateBucketRequest;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UpdateBucketRequest;

import java.util.Collections;
import java.util.Map;

/**
 * bucket.crud. Run by ../run-conformance (see ../../README.md).
 *
 * <p>Create, list, update and delete a bucket: b2_create_bucket returns the bucket with its type and
 * info (and refuses a name that is taken, duplicate_bucket_name); b2_list_buckets finds it; b2_update_bucket
 * changes the type and info and bumps the revision; b2_delete_bucket "can only delete buckets that
 * contain no version of any files" (cannot_delete_non_empty_bucket) and afterwards the bucket is gone.
 * https://www.backblaze.com/apidocs/b2-create-bucket
 * https://www.backblaze.com/apidocs/b2-list-buckets
 * https://www.backblaze.com/apidocs/b2-update-bucket
 * https://www.backblaze.com/apidocs/b2-delete-bucket
 */
public class BucketCrud {
    public static void main(String[] args) {
        Support.run("bucket.crud", () -> {
            Support.step("create");
            final String name = Support.newBucketName();
            final Map<String, String> info = Collections.singletonMap("sdkharness-bucket", "created");
            final B2Bucket created = Support.client.createBucket(B2CreateBucketRequest.builder(name, "allPrivate").setBucketInfo(info).build());
            Support.track(created);
            Support.eq("created bucketName", name, created.getBucketName());
            Support.eq("created bucketType", "allPrivate", created.getBucketType());
            Support.eq("created bucketInfo", "created", created.getBucketInfo().get("sdkharness-bucket"));
            Support.check(created.getBucketId() != null && !created.getBucketId().isEmpty(), "bucketId is empty");

            Support.step("duplicate name");
            final B2Exception duplicate = Support.expectError("create with a name already in use",
                    () -> Support.client.createBucket(name, "allPrivate"));
            Support.eq("duplicate create status", 400, duplicate.getStatus());
            Support.eq("duplicate create code", "duplicate_bucket_name", duplicate.getCode());

            Support.step("list");
            B2Bucket listed = null;
            for (B2Bucket b : Support.client.buckets()) {
                if (created.getBucketId().equals(b.getBucketId())) {
                    listed = b;
                }
            }
            Support.check(listed != null, "the new bucket is not in the bucket list");
            Support.eq("listed bucketName", name, listed.getBucketName());
            Support.eq("listed bucketType", "allPrivate", listed.getBucketType());
            final B2Bucket byName = Support.client.getBucketOrNullByName(name);
            Support.check(byName != null && created.getBucketId().equals(byName.getBucketId()), "the bucket is not found by name");

            Support.step("update");
            final B2Bucket updated = Support.client.updateBucket(B2UpdateBucketRequest.builder(created)
                    .setBucketType("allPublic")
                    .setBucketInfo(Collections.singletonMap("sdkharness-bucket", "updated"))
                    .build());
            Support.eq("updated bucketType", "allPublic", updated.getBucketType());
            Support.eq("updated bucketInfo", "updated", updated.getBucketInfo().get("sdkharness-bucket"));
            Support.check(updated.getRevision() > created.getRevision(),
                    "the revision did not increase: " + created.getRevision() + " then " + updated.getRevision());
            final B2Bucket reread = Support.client.getBucketOrNullByName(name);
            Support.check(reread != null, "the bucket disappeared after the update");
            Support.eq("re-read bucketType", "allPublic", reread.getBucketType());
            Support.eq("re-read bucketInfo", "updated", reread.getBucketInfo().get("sdkharness-bucket"));

            Support.step("delete refuses a bucket with files");
            final B2FileVersion file = Support.upload(updated, "st/bucket/file.txt", Support.utf8("in the bucket"), "text/plain", null);
            final B2Exception notEmpty = Support.expectError("delete of a bucket that holds a file",
                    () -> Support.client.deleteBucket(updated.getBucketId()));
            Support.eq("non-empty delete status", 400, notEmpty.getStatus());
            Support.eq("non-empty delete code", "cannot_delete_non_empty_bucket", notEmpty.getCode());
            Support.check(Support.client.getBucketOrNullByName(name) != null, "the bucket was deleted although it held a file");

            Support.step("delete");
            Support.client.deleteFileVersion(file);
            Support.client.deleteBucket(updated.getBucketId());
            Support.untrack(updated);
            Support.check(Support.client.getBucketOrNullByName(name) == null, "the bucket is still found by name after delete");
            for (B2Bucket b : Support.client.buckets()) {
                Support.check(!updated.getBucketId().equals(b.getBucketId()), "the deleted bucket is still in the bucket list");
            }
        });
    }
}
