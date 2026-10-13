// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

/**
 * upload.expired_token_401. Run by ../run-resilience (see ../../README.md).
 *
 * <p>A 401 on b2_upload_file (the upload authorization token expired) is answered by b2_get_upload_url
 * again, then the file lands intact. This SDK takes a new upload URL for it and does not
 * reauthorize the account, which is the documented path for an upload URL token.
 * https://www.backblaze.com/apidocs/b2-upload-file
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class UploadExpiredToken401 {
    public static void main(String[] args) {
        Support.run("upload.expired_token_401", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/upload-expired_token_401.bin";
            final byte[] data = Support.payload(2048, 401);

            Support.step("arm the fault");
            Support.armFault("b2_upload_file", 401, "expired_auth_token", null);

            Support.step("upload");
            Support.markJournal();
            Support.upload(name, data);

            Support.step("recovery path");
            Support.requireFreshUrlRetry("injected", Support.sinceMark());

            Support.step("read back");
            Support.requireIntact(name, data);
            Support.check(Support.versionsOf(name).size() == 1, "expected exactly one stored version of " + name);
        });
    }
}
