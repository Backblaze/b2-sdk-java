// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

/**
 * upload.reset_before_response. Run by ../run-resilience (see ../../README.md).
 *
 * <p>The connection is reset after the server has stored the file but before it answers, so the client
 * cannot know. It must retry on a fresh upload URL, and the file reads back intact; a second version of
 * the same name is acceptable (the first upload did land).
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class UploadResetBeforeResponse {
    public static void main(String[] args) {
        Support.run("upload.reset_before_response", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/upload-reset-before-response.bin";
            final byte[] data = Support.payload(4096, 8);

            Support.step("arm the fault");
            Support.armWireFault("b2_upload_file", "reset-before-response", null);

            Support.step("upload");
            Support.markJournal();
            Support.upload(name, data);

            Support.step("recovery path");
            Support.requireFreshUrlRetry("reset-before-response", Support.sinceMark());

            Support.step("read back");
            Support.requireIntact(name, data);
            final int versions = Support.versionsOf(name).size();
            Support.check(versions == 1 || versions == 2, "expected one or two stored versions of " + name + ", got " + versions);
            if (versions == 2) {
                Support.note("two versions of the same name: the reset came after the first upload was stored, which the scenario accepts");
            }
        });
    }
}
