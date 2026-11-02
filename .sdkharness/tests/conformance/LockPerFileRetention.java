// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileRetention;
import com.backblaze.b2.client.structures.B2FileRetentionMode;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UpdateFileRetentionRequest;

import java.util.Arrays;

/**
 * lock.per_file_retention. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_update_file_retention sets fileRetention {mode "governance" or "compliance", retainUntilTimestamp} and
 * returns it (https://www.backblaze.com/apidocs/b2-update-file-retention). "If either legal hold or retention
 * settings, or even both, are active for a given file, then that file is protected from being deleted"
 * (https://www.backblaze.com/docs/cloud-storage-object-lock), and b2_delete_file_version's bypassGovernance "Must be
 * specified and set to true if deleting a file version protected by Object Lock governance mode retention settings"
 * (https://www.backblaze.com/apidocs/b2-delete-file-version).
 *
 * <p>An object in a file-lock-enabled bucket gets a governance retention of now + 2 days. Asserted: the update's
 * response and the file's metadata report that mode and timestamp, and deleting that version without bypass is
 * refused with 401 access_denied (the answer B2's own contract test records for this call, and the simulator's) and
 * leaves the version in place. Compliance mode is never used: nothing could lift it, and the check must clean up.
 */
public class LockPerFileRetention {
    private static final long TWO_DAYS_MILLIS = 2L * 24 * 60 * 60 * 1000;

    public static void main(String[] args) {
        Support.run("lock.per_file_retention", () -> {
            Support.step("bucket and object");
            final B2Bucket bucket = Support.lockedBucket();
            final byte[] data = Support.payload(256, 82);
            final B2FileVersion file = Support.upload(bucket, "st/lock/retention.txt", data, "text/plain", null);

            Support.step("set the retention");
            final long until = System.currentTimeMillis() + TWO_DAYS_MILLIS;
            final B2FileRetention wanted = new B2FileRetention(B2FileRetentionMode.GOVERNANCE, until);
            Support.eq("retention in the update response", wanted,
                    Support.client.updateFileRetention(B2UpdateFileRetentionRequest.builder(file.getFileName(), file.getFileId(), wanted).build()).getFileRetention());

            Support.step("read back");
            final B2FileRetention reported = Support.client.getFileInfo(file.getFileId()).getFileRetention();
            Support.eq("file retention mode", B2FileRetentionMode.GOVERNANCE, reported.getMode());
            Support.eq("file retention retainUntilTimestamp", until, reported.getRetainUntilTimestamp());

            Support.step("delete without bypass is refused");
            final B2Exception refused = Support.expectError("delete of a governance-locked version",
                    () -> Support.client.deleteFileVersion(file.getFileName(), file.getFileId()));
            Support.eq("refusal status", 401, refused.getStatus());
            Support.eq("refusal code", "access_denied", refused.getCode());

            Support.step("the version is still there");
            Support.check(Arrays.equals(data, Support.downloadById(file.getFileId()).bytes), "the refused delete changed the file");
        });
    }
}
