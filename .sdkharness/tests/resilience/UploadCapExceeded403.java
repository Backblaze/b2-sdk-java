// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import com.backblaze.b2.client.exceptions.B2Exception;
import java.util.List;

/**
 * upload.cap_exceeded_403. Run by ../run-resilience (see ../../README.md).
 *
 * <p>A 403 cap_exceeded on b2_upload_file is a hard stop, not a transient fault: the error reaches the
 * caller and the upload is not retried. The journal shows exactly one b2_upload_file, and nothing
 * was stored.
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class UploadCapExceeded403 {
    public static void main(String[] args) {
        Support.run("upload.cap_exceeded_403", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/upload-cap-exceeded-403.bin";

            Support.step("arm the fault");
            Support.armFault("b2_upload_file", 403, "cap_exceeded", null);

            Support.step("upload");
            Support.markJournal();
            B2Exception error = null;
            try {
                Support.upload(name, Support.payload(2048, 403));
            } catch (B2Exception e) {
                error = e;
            }
            Support.check(error != null, "the upload succeeded although the simulator answered 403 cap_exceeded");
            Support.check(error.getStatus() == 403, "the error that reached the caller was not a 403: " + Support.describe(error));
            Support.check("cap_exceeded".equals(error.getCode()), "the error that reached the caller lost its code: " + Support.describe(error));

            Support.step("recovery path");
            final List<Support.Entry> window = Support.sinceMark();
            final List<Support.Entry> uploads = Support.only(window, "b2_upload_file");
            Support.check(uploads.size() == 1, "expected exactly one b2_upload_file (no retry), got " + uploads.size() + ": " + Support.show(window));
            Support.check(uploads.get(0).status == 403 && uploads.get(0).fault.equals("injected"), "the single upload was not the faulted one: " + Support.show(window));

            Support.step("nothing stored");
            Support.check(Support.versionsOf(name).isEmpty(), "a version of " + name + " exists although the upload was refused");
        });
    }
}
