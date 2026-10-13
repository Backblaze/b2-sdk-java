// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

/**
 * upload.retry_408. Run by ../run-resilience (see ../../README.md).
 *
 * <p>A 408 on b2_upload_file is retried the same way: a fresh upload URL, then the file lands intact.
 * https://www.backblaze.com/apidocs/b2-upload-file
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class UploadRetry408 {
    public static void main(String[] args) {
        Support.run("upload.retry_408", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/upload-retry_408.bin";
            final byte[] data = Support.payload(2048, 408);

            Support.step("arm the fault");
            Support.armFault("b2_upload_file", 408, "request_timeout", null);

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
