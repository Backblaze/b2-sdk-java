/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

public interface B2Sdk {
    /**
     * @return the name of this sdk.
     *         it's a string that matches [a-zA-Z][-_.a-zA-Z0-9]*
     */
    static String getName() {
        return "b2-sdk-java";
    }

    /**
     * @return the version of this SDK.
     *         it's a string that matches [0-9][0-9.]*
     */
    static String getVersion() {
        return "0.0.5";
    }
}
