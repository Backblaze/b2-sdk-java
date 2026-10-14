// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import java.util.Arrays;
import java.util.List;

/**
 * download.retry_503. Run by ../run-resilience (see ../../README.md).
 *
 * <p>A 503 on b2_download_file_by_id is retried, and the bytes delivered are intact.
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class DownloadRetry503 {
    public static void main(String[] args) {
        Support.run("download.retry_503", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/download-retry-503.bin";
            final byte[] data = Support.payload(4096, 14);
            final String fileId = Support.upload(name, data).getFileId();

            Support.step("arm the fault");
            Support.armFault("b2_download_file_by_id", 503, "service_unavailable", null);

            Support.step("download");
            Support.markJournal();
            final byte[] got = Support.downloadById(fileId);

            Support.step("recovery path");
            final List<Support.Entry> window = Support.only(Support.sinceMark(), "b2_download_file_by_id");
            Support.check(window.size() >= 2 && window.get(0).status == 503 && window.get(0).fault.equals("injected"),
                    "the first b2_download_file_by_id did not carry the 503 fault: " + Support.show(window));
            Support.check(window.get(window.size() - 1).status == 200, "the download was never retried to a 200: " + Support.show(window));

            Support.step("bytes");
            Support.check(Arrays.equals(data, got), "the downloaded " + got.length + " bytes differ from the " + data.length + " uploaded");
        });
    }
}
