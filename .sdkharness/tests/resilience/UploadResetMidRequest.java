// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

/**
 * upload.reset_mid_request. Run by ../run-resilience (see ../../README.md).
 *
 * <p>The connection is reset while the body is being sent. The client treats it as a failed upload, asks for
 * a fresh upload URL and retries; the file lands intact.
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class UploadResetMidRequest {
    public static void main(String[] args) {
        Support.run("upload.reset_mid_request", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/upload-reset-mid-request.bin";
            final byte[] data = Support.payload(4096, 7);

            Support.step("arm the fault");
            Support.armWireFault("b2_upload_file", "reset-mid-request", null);

            Support.step("upload");
            Support.markJournal();
            Support.upload(name, data);

            Support.step("recovery path");
            Support.requireFreshUrlRetry("reset-mid-request", Support.sinceMark());

            Support.step("read back");
            Support.requireIntact(name, data);
            Support.check(Support.versionsOf(name).size() == 1, "expected exactly one stored version of " + name);
        });
    }
}
