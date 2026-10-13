// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import java.util.List;

/**
 * part.retry_503. Run by ../run-resilience (see ../../README.md).
 *
 * <p>A 503 on b2_upload_part is retried: the part goes to a different upload part URL, and the large file
 * finishes with its bytes intact. The journal shows the faulted part, a successful b2_upload_part on a
 * different URL id, and b2_finish_large_file.
 * https://www.backblaze.com/apidocs/b2-upload-part
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class PartRetry503 {
    public static void main(String[] args) {
        Support.run("part.retry_503", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/part-retry-503.bin";
            // Several parts at the simulator's 5000-byte part size, so a part can be retried while others proceed.
            final byte[] data = Support.payload(16000, 10);

            Support.step("arm the fault");
            Support.armFault("b2_upload_part", 503, "service_unavailable", null);

            Support.step("upload");
            Support.markJournal();
            Support.uploadLarge(name, data);

            Support.step("recovery path");
            final List<Support.Entry> window = Support.sinceMark();
            final List<Support.Entry> parts = Support.only(window, "b2_upload_part");
            Support.Entry faulted = null;
            for (Support.Entry e : parts) {
                if (e.fault.equals("injected") && e.status == 503) {
                    faulted = e;
                }
            }
            Support.check(faulted != null, "no b2_upload_part carried the 503 fault: " + Support.show(window));
            int later = 0;
            for (Support.Entry e : parts.subList(parts.indexOf(faulted) + 1, parts.size())) {
                if (e.status == 200 && !e.uploadUrlId.isEmpty() && !e.uploadUrlId.equals(faulted.uploadUrlId)) {
                    later++;
                }
            }
            Support.check(later > 0, "no b2_upload_part succeeded on a part URL other than the faulted one (" + faulted.uploadUrlId + "): " + Support.show(window));
            Support.check(Support.indexOf(window, 0, "b2_finish_large_file") >= 0, "the large file was never finished: " + Support.show(window));

            Support.step("read back");
            Support.requireIntact(name, data);
        });
    }
}
