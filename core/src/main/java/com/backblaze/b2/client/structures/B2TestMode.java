/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */

package com.backblaze.b2.client.structures;

/**
 * TestModes allow the client to ask the server to cause errors to help
 * with testing the client.  Don't want to use them in your production
 * code.
 */
public enum B2TestMode {
    FAIL_SOME_UPLOADS("fail_some_uploads"),
    FORCE_CAP_EXCEEDED("force_cap_exceeded"),
    EXPIRE_SOME_ACCOUNT_AUTHORIZATION_TOKENS("expire_some_account_authorization_tokens");

    private final String valueForHeader;

    B2TestMode(String valueForHeader) {
        this.valueForHeader = valueForHeader;
    }

    // this should really only be used internally, but the SDK needs it from other packages.
    public String getValueForHeader() {
        return valueForHeader;
    }
}
