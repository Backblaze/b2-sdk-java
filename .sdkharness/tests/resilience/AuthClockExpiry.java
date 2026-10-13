// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import java.util.List;

/**
 * auth.clock_expiry. Run by ../run-resilience (see ../../README.md).
 *
 * <p>One long-lived client (B2StorageClient is one) keeps working past the 24 h life of its account token:
 * the simulator's clock is moved a day and a millisecond forward, the real 401 comes back, and the
 * client calls b2_authorize_account again and the listing succeeds. No fault is injected; the 401 is the
 * simulator's own.
 * https://www.backblaze.com/apidocs/b2-authorize-account
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class AuthClockExpiry {
    public static void main(String[] args) {
        Support.run("auth.clock_expiry", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/auth-clock-expiry.bin";
            Support.upload(name, Support.payload(1024, 12));
            // One call first, so the client is holding the token that is about to expire.
            Support.check(Support.listNames().contains(name), "the listing before the clock moved did not contain " + name);

            Support.step("move the clock");
            Support.advanceClock(86400001L);

            Support.step("list");
            Support.markJournal();
            final List<String> names = Support.listNames();

            Support.step("recovery path");
            final List<Support.Entry> window = Support.sinceMark();
            final int hit = Support.indexOf(window, 0, "b2_list_file_names");
            Support.check(hit >= 0 && window.get(hit).status == 401 && window.get(hit).fault.isEmpty(),
                    "the first b2_list_file_names was not the simulator's own 401: " + Support.show(window));
            final int auth = Support.indexOf(window, hit + 1, "b2_authorize_account");
            Support.check(auth >= 0 && window.get(auth).status == 200, "no b2_authorize_account after the 401: " + Support.show(window));
            final int retry = Support.indexOf(window, auth + 1, "b2_list_file_names");
            Support.check(retry >= 0 && window.get(retry).status == 200, "no successful b2_list_file_names after reauthorizing: " + Support.show(window));

            Support.step("listing");
            Support.check(names.contains(name), "the listing did not contain " + name + ": " + names);
        });
    }
}
