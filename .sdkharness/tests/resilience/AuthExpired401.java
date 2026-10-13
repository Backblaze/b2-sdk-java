// Copyright 2026, Backblaze Inc. All Rights Reserved.
// License https://www.backblaze.com/using_b2_code.html

import java.util.List;

/**
 * auth.expired_401. Run by ../run-resilience (see ../../README.md).
 *
 * <p>A 401 expired_auth_token on an API call is answered by b2_authorize_account again, and the listing then
 * succeeds. The journal shows the faulted b2_list_file_names, an authorize, and a successful listing.
 * https://www.backblaze.com/apidocs/b2-list-file-names
 * https://www.backblaze.com/docs/cloud-storage-integration-checklist
 */
public class AuthExpired401 {
    public static void main(String[] args) {
        Support.run("auth.expired_401", () -> {
            Support.step("setup");
            Support.bucket();
            final String name = "res/auth-expired-401.bin";
            Support.upload(name, Support.payload(1024, 11));

            Support.step("arm the fault");
            Support.armFault("b2_list_file_names", 401, "expired_auth_token", null);

            Support.step("list");
            Support.markJournal();
            final List<String> names = Support.listNames();

            Support.step("recovery path");
            final List<Support.Entry> window = Support.sinceMark();
            final int hit = Support.indexOf(window, 0, "b2_list_file_names");
            Support.check(hit >= 0 && window.get(hit).status == 401 && window.get(hit).fault.equals("injected"),
                    "the first b2_list_file_names did not carry the 401 fault: " + Support.show(window));
            final int auth = Support.indexOf(window, hit + 1, "b2_authorize_account");
            Support.check(auth >= 0 && window.get(auth).status == 200, "no b2_authorize_account after the 401: " + Support.show(window));
            final int retry = Support.indexOf(window, auth + 1, "b2_list_file_names");
            Support.check(retry >= 0 && window.get(retry).status == 200, "no successful b2_list_file_names after reauthorizing: " + Support.show(window));

            Support.step("listing");
            Support.check(names.contains(name), "the listing did not contain " + name + ": " + names);
        });
    }
}
