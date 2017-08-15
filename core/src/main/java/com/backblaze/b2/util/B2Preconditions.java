/*
 * Copyright 2017, Backblaze Inc. All Rights Reserved.
 * License https://www.backblaze.com/using_b2_code.html
 */
package com.backblaze.b2.util;

public interface B2Preconditions {

    static void checkArgument(boolean expr) {
        if (!expr) {
            throw new IllegalArgumentException();
        }
    }
    static void checkArgument(boolean expr, Object msg) {
        if (!expr) {
            throw new IllegalArgumentException(String.valueOf(msg));
        }
    }


    static void checkState(boolean expr) {
        if (!expr) {
            throw new IllegalStateException();
        }
    }
    static void checkState(boolean expr, Object msg) {
        if (!expr) {
            throw new IllegalStateException(String.valueOf(msg));
        }
    }

    static void checkArgumentIsNotNull(Object argument, Object argumentName) {
        if (argument == null) {
            throw new IllegalArgumentException("argument " + argumentName + " must not be null!");
        }
    }
}
