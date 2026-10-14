// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

/**
 * upload.retry_503. Run by ../run-resilience (see ../../README.md).
 *
 * <p>A 503 on b2_upload_file means the upload URL is not usable right now: the client must ask
 * b2_get_upload_url for a fresh one and retry, and the file must land intact. The journal shows the
 * retried b2_upload_file on a new upload URL id.
 * https://www.backblaze.com/apidocs/b2-upload-file
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class UploadRetry503 {
    public static void main(String[] args) {
        Support.run("upload.retry_503", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/upload-retry_503.bin";
            final byte[] data = Support.payload(2048, 503);

            Support.step("arm the fault");
            Support.armFault("b2_upload_file", 503, "service_unavailable", null);

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
