// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import java.util.ArrayList;
import java.util.List;

/**
 * api.retry_after_429. Run by ../run-resilience (see ../../README.md).
 *
 * <p>A 429 with Retry-After: 2 is not retried sooner than 2 seconds later (a lower bound on the whole
 * call), and the listing then succeeds.
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class ApiRetryAfter429 {
    public static void main(String[] args) {
        Support.run("api.retry_after_429", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/api-retry_after_429.bin";
            Support.upload(name, Support.payload(1024, 429));

            Support.step("arm the fault");
            Support.armFault("b2_list_file_names", 429, "too_many_requests", 2);

            Support.step("list");
            Support.markJournal();
            final List<String> names = new ArrayList<>();
            final long elapsedMs = Support.timed(() -> names.addAll(Support.listNames()));

            Support.step("recovery path");
            final List<Support.Entry> window = Support.only(Support.sinceMark(), "b2_list_file_names");
            Support.check(window.size() >= 2 && window.get(0).status == 429 && window.get(0).fault.equals("injected"),
                    "the first b2_list_file_names did not carry the 429 fault: " + Support.show(window));
            Support.check(window.get(window.size() - 1).status == 200, "the listing was never retried to a 200: " + Support.show(window));

            Support.step("retry-after");
            // Lower bound only: the journal has no timestamps, so the whole call is timed, and an upper bound would be flaky.
            Support.check(elapsedMs >= 2000, "the retry came after " + elapsedMs + " ms, sooner than the 2 s Retry-After");
            Support.note("the call took " + elapsedMs + " ms; only the 2 s lower bound is asserted, which is weak evidence of honouring Retry-After");

            Support.step("listing");
            Support.check(names.contains(name), "the listing did not contain " + name + ": " + names);
        });
    }
}
