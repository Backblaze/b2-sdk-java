// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

/**
 * upload.stall. Run by ../run-resilience (see ../../README.md).
 *
 * <p>The server holds a connection open and sends nothing (60 s). A client with a request timeout must
 * abandon the stalled request and retry on a fresh upload URL, and the file lands intact. This scenario
 * needs a request timeout shorter than the stall: the SDK offers one, a documented builder option
 * (HttpClientFactoryImpl.Builder.setSocketTimeoutSeconds), so the check sets 2 s and must PASS or
 * FAIL, never no-client-option. The default is 20 s.
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class UploadStall {
    public static void main(String[] args) {
        Support.run("upload.stall", 2, () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/upload-stall.bin";
            final byte[] data = Support.payload(4096, 9);

            Support.step("arm the fault");
            Support.armWireFault("b2_upload_file", "stall", 60000);

            Support.step("upload");
            Support.markJournal();
            Support.upload(name, data);

            Support.step("recovery path");
            Support.requireFreshUrlRetry("stall", Support.sinceMark());

            Support.step("read back");
            Support.requireIntact(name, data);
        });
    }
}
