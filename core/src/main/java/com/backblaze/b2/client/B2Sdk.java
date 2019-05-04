/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.client;

import com.backblaze.b2.util.B2Preconditions;
import com.backblaze.b2.util.B2StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class B2Sdk {
    // caches the answer to getVersion().  only access with getVersion().
    private static String version;

    private static String readVersion() {
        try (final InputStream in = B2Sdk.class.getClassLoader().getResourceAsStream("b2-sdk-core/version.txt");
             final BufferedReader reader = new BufferedReader(new InputStreamReader(in, B2StringUtil.UTF8))) {
            final String version = reader.readLine().trim();
            B2Preconditions.checkState(!version.isEmpty());
            return version;
        } catch (IOException e) {
            throw new RuntimeException("failed to read sdk version: " + e, e);
        }
    }

    /**
     * @return the name of this sdk.
     *         it's a string that matches [a-zA-Z][-_.a-zA-Z0-9]*
     */
    public static String getName() {
        return "b2-sdk-java";
    }

    /**
     * @return the version of this SDK.
     *         it's a string that matches [0-9][0-9.]*
     */
    public static synchronized String getVersion() {
        if (version == null) {
            version = readVersion();
        }

        return version;
    }
}
