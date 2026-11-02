// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2DeleteFileVersionRequest;
import com.backblaze.b2.client.structures.B2FileRetention;
import com.backblaze.b2.client.structures.B2FileRetentionMode;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UpdateFileRetentionRequest;

import java.util.Arrays;

/**
 * lock.bypass_governance. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_delete_file_version's bypassGovernance "Must be specified and set to true if deleting a file version
 * protected by Object Lock governance mode retention settings. Setting the value requires the bypassGovernance
 * application key capability." Without it the delete is refused: 401 access_denied, "The delete was not allowed
 * because Object Lock is enabled on the file." (https://www.backblaze.com/apidocs/b2-delete-file-version). The
 * account key the simulator hands every check holds the capability.
 *
 * <p>An object under governance retention (now + 2 days) is deleted without bypass, then with
 * {@code setBypassGovernance(true)}. Asserted: the first is refused with 401 access_denied and leaves the version
 * in place; the second succeeds and the version is gone from the listing and from download by id. The SDK treats a
 * 401 on a call other than an upload as a reason to reauthorize and retry; the retries end in the same refusal,
 * which is what the check asserts, so the extra requests are not a failure.
 */
public class LockBypassGovernance {
    public static void main(String[] args) {
        Support.run("lock.bypass_governance", () -> {
            Support.step("bucket and object under governance");
            final B2Bucket bucket = Support.lockedBucket();
            final byte[] data = Support.payload(256, 84);
            final B2FileVersion file = Support.upload(bucket, "st/lock/bypass.txt", data, "text/plain", null);
            Support.client.updateFileRetention(B2UpdateFileRetentionRequest.builder(file.getFileName(), file.getFileId(),
                    new B2FileRetention(B2FileRetentionMode.GOVERNANCE, System.currentTimeMillis() + 2L * 24 * 60 * 60 * 1000)).build());

            Support.step("delete without bypass is refused");
            final B2Exception refused = Support.expectError("delete of a governance-locked version without bypass",
                    () -> Support.client.deleteFileVersion(B2DeleteFileVersionRequest.builder(file.getFileName(), file.getFileId()).build()));
            Support.eq("refusal status", 401, refused.getStatus());
            Support.eq("refusal code", "access_denied", refused.getCode());
            Support.check(Arrays.equals(data, Support.downloadById(file.getFileId()).bytes), "the refused delete changed the file");

            Support.step("delete with bypass");
            Support.client.deleteFileVersion(B2DeleteFileVersionRequest.builder(file.getFileName(), file.getFileId()).setBypassGovernance(true).build());

            Support.step("the version is gone");
            Support.eq("versions left after the bypassing delete", 0, Support.versions(bucket.getBucketId(), file.getFileName()).size());
            Support.eq("download by id of the deleted version", 404, Support.expectError("download of a deleted version",
                    () -> Support.downloadById(file.getFileId())).getStatus());
        });
    }
}
