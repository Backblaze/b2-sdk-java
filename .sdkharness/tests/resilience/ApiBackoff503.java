// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import java.util.ArrayList;
import java.util.List;

/**
 * api.backoff_503. Run by ../run-resilience (see ../../README.md).
 *
 * <p>A 503 with no Retry-After is retried after the documented first backoff step, 1 second (a lower bound
 * on the whole call), and the listing then succeeds.
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class ApiBackoff503 {
    public static void main(String[] args) {
        Support.run("api.backoff_503", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/api-backoff-503.bin";
            Support.upload(name, Support.payload(1024, 13));

            Support.step("arm the fault");
            Support.armFault("b2_list_file_names", 503, "service_unavailable", null);

            Support.step("list");
            Support.markJournal();
            final List<String> names = new ArrayList<>();
            final long elapsedMs = Support.timed(() -> names.addAll(Support.listNames()));

            Support.step("recovery path");
            final List<Support.Entry> window = Support.only(Support.sinceMark(), "b2_list_file_names");
            Support.check(window.size() >= 2 && window.get(0).status == 503 && window.get(0).fault.equals("injected"),
                    "the first b2_list_file_names did not carry the 503 fault: " + Support.show(window));
            Support.check(window.get(window.size() - 1).status == 200, "the listing was never retried to a 200: " + Support.show(window));

            Support.step("backoff");
            // Lower bound only, for the same reason as the Retry-After checks.
            Support.check(elapsedMs >= 1000, "the retry came after " + elapsedMs + " ms, sooner than the 1 s first backoff step");
            Support.note("the call took " + elapsedMs + " ms; only the 1 s lower bound is asserted, which is weak evidence");

            Support.step("listing");
            Support.check(names.contains(name), "the listing did not contain " + name + ": " + names);
        });
    }
}
