// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2Bucket;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2LegalHold;
import com.backblaze.b2.client.structures.B2UpdateFileLegalHoldRequest;

import java.util.Arrays;

/**
 * lock.legal_hold. Run by ../run-conformance (see ../../README.md).
 *
 * <p>b2_update_file_legal_hold takes legalHold "on" or "off" and is "Used to enable legal hold for a file in an
 * Object Lock-enabled bucket, preventing it from being deleted, or to disable legal hold protections"
 * (https://www.backblaze.com/apidocs/b2-update-file-legal-hold). "If either legal hold or retention settings, or even
 * both, are active for a given file, then that file is protected from being deleted"
 * (https://www.backblaze.com/docs/cloud-storage-object-lock).
 *
 * <p>An object in a file-lock-enabled bucket: the hold is set on, read back, and set off. Asserted: while on, the
 * update's response and the metadata say on, deleting the version is refused (B2 documents the protection, not the
 * status, so a 400, 401, 403 or 409 service error counts) and the file is still there; once off, the metadata says
 * off and deleting the version is permitted and removes it.
 */
public class LockLegalHold {
    public static void main(String[] args) {
        Support.run("lock.legal_hold", () -> {
            Support.step("bucket and object");
            final B2Bucket bucket = Support.lockedBucket();
            final byte[] data = Support.payload(256, 83);
            final B2FileVersion file = Support.upload(bucket, "st/lock/hold.txt", data, "text/plain", null);

            Support.step("hold on");
            Support.eq("legal hold in the update response", B2LegalHold.ON, Support.client.updateFileLegalHold(
                    B2UpdateFileLegalHoldRequest.builder(file.getFileName(), file.getFileId(), B2LegalHold.ON).build()).getLegalHold());
            Support.eq("legal hold in the metadata", B2LegalHold.ON, Support.client.getFileInfo(file.getFileId()).getLegalHold());

            Support.step("delete while held is refused");
            final B2Exception refused = Support.expectError("delete of a version under legal hold",
                    () -> Support.client.deleteFileVersion(file.getFileName(), file.getFileId()));
            Support.check(Arrays.asList(400, 401, 403, 409).contains(refused.getStatus()),
                    "refused with status " + refused.getStatus() + " code " + refused.getCode() + ", expected 400, 401, 403 or 409");
            Support.check(Arrays.equals(data, Support.downloadById(file.getFileId()).bytes), "the refused delete changed the file");

            Support.step("hold off");
            Support.eq("legal hold in the update response", B2LegalHold.OFF, Support.client.updateFileLegalHold(
                    B2UpdateFileLegalHoldRequest.builder(file.getFileName(), file.getFileId(), B2LegalHold.OFF).build()).getLegalHold());
            Support.eq("legal hold in the metadata", B2LegalHold.OFF, Support.client.getFileInfo(file.getFileId()).getLegalHold());

            Support.step("delete once released");
            Support.client.deleteFileVersion(file.getFileName(), file.getFileId());
            Support.eq("versions left after the delete", 0, Support.versions(bucket.getBucketId(), file.getFileName()).size());
        });
    }
}
