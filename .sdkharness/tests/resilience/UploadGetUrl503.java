// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import java.util.List;

/**
 * upload.get_url_503. Run by ../run-resilience (see ../../README.md).
 *
 * <p>A 503 on b2_get_upload_url is retried, then the file lands intact. The fault is armed before the
 * first upload in the bucket: the SDK pools upload URLs, so after any earlier upload it would reuse one
 * and never call b2_get_upload_url at all (the harness then rejects a PASS its journal does not back).
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class UploadGetUrl503 {
    public static void main(String[] args) {
        Support.run("upload.get_url_503", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/upload-get-url-503.bin";
            final byte[] data = Support.payload(2048, 5);

            Support.step("arm the fault");
            // Before the first upload in this bucket: nothing is pooled yet, so the first upload must ask for a URL.
            Support.armFault("b2_get_upload_url", 503, "service_unavailable", null);

            Support.step("upload");
            Support.markJournal();
            Support.upload(name, data);

            Support.step("recovery path");
            final List<Support.Entry> window = Support.sinceMark();
            final int hit = Support.indexOf(window, 0, "b2_get_upload_url");
            Support.check(hit >= 0 && window.get(hit).status == 503 && window.get(hit).fault.equals("injected"),
                    "the first b2_get_upload_url did not carry the 503 fault: " + Support.show(window));
            final int again = Support.indexOf(window, hit + 1, "b2_get_upload_url");
            Support.check(again >= 0 && window.get(again).status == 200,
                    "b2_get_upload_url was not retried to a 200: " + Support.show(window));
            final int sent = Support.indexOf(window, again + 1, "b2_upload_file");
            Support.check(sent >= 0 && window.get(sent).status == 200, "no successful b2_upload_file after the retry: " + Support.show(window));

            Support.step("read back");
            Support.requireIntact(name, data);
        });
    }
}
